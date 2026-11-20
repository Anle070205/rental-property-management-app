package com.roomcare.backend.service;

import com.roomcare.backend.entity.AuthVerificationToken;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.exception.TooManyRequestsException;
import com.roomcare.backend.repository.AuthVerificationTokenRepository;
import com.roomcare.backend.util.PhoneNumberUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Quản lý trọn vòng đời OTP cho cả Email (Gmail) và Số điện thoại (+84):
 * - OTP 6 chữ số, hết hạn mặc định 5 phút, tối đa 5 lần nhập sai, sử dụng 1 lần
 * - Rate limit theo tài khoản (target), IP (client_ip) và phương thức (channel)
 * - Lưu SHA-256 + pepper (hoặc PROVIDER_MANAGED cho Twilio Verify), tuyệt đối không lưu plaintext
 */
@Service
public class OtpVerificationService {

    public record IssuedOtp(AuthVerificationToken token, String rawOtpOrNull, boolean providerManaged) {}

    private final AuthVerificationTokenRepository tokenRepository;
    private final OtpAttemptService otpAttemptService;
    private final SmsOtpProvider smsOtpProvider;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int otpExpirationMinutes;
    private final int resendCooldownSeconds;
    private final int maxRequestsPerHour;
    private final int maxRequestsPerIpPerHour;
    private final int maxAttempts;
    private final String tokenPepper;

    public OtpVerificationService(
            AuthVerificationTokenRepository tokenRepository,
            OtpAttemptService otpAttemptService,
            SmsOtpProvider smsOtpProvider,
            @Value("${roomcare.auth.otp-expiration-minutes:5}") int otpExpirationMinutes,
            @Value("${roomcare.auth.otp-resend-cooldown-seconds:60}") int resendCooldownSeconds,
            @Value("${roomcare.auth.otp-max-requests-per-hour:5}") int maxRequestsPerHour,
            @Value("${roomcare.auth.otp-max-requests-per-ip-per-hour:20}") int maxRequestsPerIpPerHour,
            @Value("${roomcare.auth.otp-max-attempts:5}") int maxAttempts,
            @Value("${roomcare.auth.token-pepper:roomcare-default-pepper}") String tokenPepper
    ) {
        this.tokenRepository = tokenRepository;
        this.otpAttemptService = otpAttemptService;
        this.smsOtpProvider = smsOtpProvider;
        this.otpExpirationMinutes = Math.max(1, otpExpirationMinutes);
        this.resendCooldownSeconds = Math.max(1, resendCooldownSeconds);
        this.maxRequestsPerHour = Math.max(1, maxRequestsPerHour);
        this.maxRequestsPerIpPerHour = Math.max(1, maxRequestsPerIpPerHour);
        this.maxAttempts = Math.max(1, maxAttempts);
        this.tokenPepper = tokenPepper != null ? tokenPepper : "";
    }

    public int getOtpExpirationMinutes() {
        return otpExpirationMinutes;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    /**
     * Chuẩn hóa đích nhận OTP theo đúng phương thức (email lower-case hoặc +84...).
     */
    public String normalizeTarget(AuthVerificationToken.Channel channel, String rawTarget) {
        if (channel == null) {
            throw new RuntimeException("Phương thức xác thực không hợp lệ");
        }
        String trimmed = rawTarget != null ? rawTarget.trim() : "";
        return switch (channel) {
            case EMAIL -> trimmed.toLowerCase(Locale.ROOT);
            case PHONE -> PhoneNumberUtils.normalizeToE164(trimmed);
        };
    }

    @Transactional
    public IssuedOtp createTokenAndDispatchPreparation(
            User user,
            AuthVerificationToken.Channel channel,
            AuthVerificationToken.Purpose purpose,
            String rawTarget,
            String clientIp
    ) {
        String normalizedTarget = normalizeTarget(channel, rawTarget);
        String cleanIp = StringUtils.hasText(clientIp) ? clientIp.trim() : null;
        LocalDateTime now = LocalDateTime.now();

        enforceRateLimits(normalizedTarget, channel, cleanIp, now);
        invalidateActiveTokens(user.getId(), channel, purpose);

        boolean providerManaged = channel == AuthVerificationToken.Channel.PHONE
                && smsOtpProvider != null
                && smsOtpProvider.isProviderManagedVerification();

        String rawOtp = providerManaged ? null : generateNumericOtp();
        String codeHash = providerManaged
                ? hashSecret(channel, purpose, normalizedTarget, "PROVIDER_MANAGED:" + generateHexTicket())
                : hashSecret(channel, purpose, normalizedTarget, rawOtp);

        AuthVerificationToken token = new AuthVerificationToken();
        token.setUser(user);
        token.setChannel(channel);
        token.setPurpose(purpose);
        token.setTarget(normalizedTarget);
        token.setCodeHash(codeHash);
        token.setProvider(resolveProviderName(channel));
        token.setClientIp(cleanIp);
        token.setExpiresAt(now.plusMinutes(otpExpirationMinutes));
        token.setAttemptCount(0);
        token.setMaxAttempts(maxAttempts);
        token.setInvalidated(false);

        AuthVerificationToken saved = tokenRepository.save(token);
        return new IssuedOtp(saved, rawOtp, providerManaged);
    }

    @Transactional
    public void attachProviderReference(Long tokenId, String providerReference) {
        if (tokenId == null || !StringUtils.hasText(providerReference)) {
            return;
        }
        tokenRepository.findById(tokenId).ifPresent(token -> {
            token.setProviderReference(providerReference);
            tokenRepository.save(token);
        });
    }

    @Transactional
    public AuthVerificationToken verifyOtp(
            AuthVerificationToken.Channel channel,
            AuthVerificationToken.Purpose purpose,
            String rawTarget,
            String rawOtp
    ) {
        AuthVerificationToken token = verifyOtpInternal(channel, purpose, rawTarget, rawOtp);
        token.setUsedAt(LocalDateTime.now());
        token.setInvalidated(true);
        return tokenRepository.save(token);
    }

    @Transactional
    public String verifyResetOtpAndIssueTicket(
            AuthVerificationToken.Channel channel,
            String rawTarget,
            String rawOtp
    ) {
        AuthVerificationToken token = verifyOtpInternal(
                channel,
                AuthVerificationToken.Purpose.PASSWORD_RESET,
                rawTarget,
                rawOtp
        );
        String resetTicket = generateHexTicket();
        token.setResetTicketHash(hashSecret(
                channel,
                AuthVerificationToken.Purpose.PASSWORD_RESET,
                token.getTarget(),
                resetTicket
        ));
        tokenRepository.save(token);
        return resetTicket;
    }

    @Transactional
    public AuthVerificationToken consumeResetTicket(
            AuthVerificationToken.Channel channel,
            String rawTarget,
            String resetTicket
    ) {
        String normalizedTarget = normalizeTarget(channel, rawTarget);
        String cleanTicket = resetTicket != null ? resetTicket.trim() : "";
        String expectedHash = hashSecret(
                channel,
                AuthVerificationToken.Purpose.PASSWORD_RESET,
                normalizedTarget,
                cleanTicket
        );

        AuthVerificationToken token = tokenRepository
                .findTopByTargetIgnoreCaseAndChannelAndPurposeAndResetTicketHashAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
                        normalizedTarget,
                        channel,
                        AuthVerificationToken.Purpose.PASSWORD_RESET,
                        expectedHash
                )
                .orElseThrow(() -> new RuntimeException("Phiên đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setInvalidated(true);
            tokenRepository.save(token);
            throw new RuntimeException("Mã đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        token.setUsedAt(LocalDateTime.now());
        token.setInvalidated(true);
        AuthVerificationToken saved = tokenRepository.save(token);
        if (saved.getUser() != null && saved.getUser().getId() != null) {
            invalidateActiveTokens(saved.getUser().getId(), channel, AuthVerificationToken.Purpose.PASSWORD_RESET);
        }
        return saved;
    }

    private AuthVerificationToken verifyOtpInternal(
            AuthVerificationToken.Channel channel,
            AuthVerificationToken.Purpose purpose,
            String rawTarget,
            String rawOtp
    ) {
        String normalizedTarget = normalizeTarget(channel, rawTarget);
        String cleanOtp = rawOtp != null ? rawOtp.trim() : "";

        AuthVerificationToken token = tokenRepository
                .findTopByTargetIgnoreCaseAndChannelAndPurposeAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
                        normalizedTarget,
                        channel,
                        purpose
                )
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy mã OTP còn hiệu lực cho phương thức và mục đích này. Vui lòng gửi lại mã mới."
                ));

        LocalDateTime now = LocalDateTime.now();
        if (token.getExpiresAt().isBefore(now)) {
            throw new RuntimeException("Mã OTP đã hết hạn (hiệu lực " + otpExpirationMinutes + " phút). Vui lòng gửi lại mã mới.");
        }

        int currentAttempts = token.getAttemptCount() != null ? token.getAttemptCount() : 0;
        int limit = token.getMaxAttempts() != null && token.getMaxAttempts() > 0 ? token.getMaxAttempts() : maxAttempts;
        if (currentAttempts >= limit) {
            throw new RuntimeException("Mã OTP đã bị vô hiệu hóa do nhập sai quá " + limit + " lần. Vui lòng yêu cầu mã mới.");
        }

        boolean matches;
        if ("TWILIO_VERIFY".equalsIgnoreCase(token.getProvider())
                && smsOtpProvider != null
                && smsOtpProvider.isProviderManagedVerification()) {
            matches = smsOtpProvider.verifyProviderOtp(normalizedTarget, cleanOtp);
        } else {
            String candidateHash = hashSecret(channel, purpose, normalizedTarget, cleanOtp);
            matches = constantTimeEquals(token.getCodeHash(), candidateHash);
        }

        if (!matches) {
            int remaining = otpAttemptService.recordFailedAttempt(token.getId());
            if (remaining <= 0) {
                throw new RuntimeException("Bạn đã nhập sai OTP quá " + limit + " lần. Vui lòng yêu cầu mã mới.");
            }
            throw new RuntimeException("Mã OTP không chính xác. Bạn còn " + remaining + " lần thử.");
        }

        return token;
    }

    private void enforceRateLimits(
            String normalizedTarget,
            AuthVerificationToken.Channel channel,
            String clientIp,
            LocalDateTime now
    ) {
        tokenRepository.findTopByTargetIgnoreCaseAndChannelOrderByCreatedAtDescIdDesc(normalizedTarget, channel)
                .ifPresent(latest -> {
                    if (latest.getCreatedAt() != null) {
                        long elapsedSeconds = Duration.between(latest.getCreatedAt(), now).getSeconds();
                        if (elapsedSeconds >= 0 && elapsedSeconds < resendCooldownSeconds) {
                            long waitSeconds = resendCooldownSeconds - elapsedSeconds;
                            throw new TooManyRequestsException(
                                    "Bạn thao tác quá nhanh. Vui lòng đợi " + waitSeconds + " giây trước khi gửi lại mã.",
                                    waitSeconds
                            );
                        }
                    }
                });

        long targetCount = tokenRepository.countByTargetIgnoreCaseAndChannelAndCreatedAtAfter(
                normalizedTarget,
                channel,
                now.minusHours(1)
        );
        if (targetCount >= maxRequestsPerHour) {
            throw new TooManyRequestsException(
                    "Tài khoản đã vượt quá giới hạn " + maxRequestsPerHour + " lần gửi mã trong 1 giờ. Vui lòng thử lại sau.",
                    300
            );
        }

        if (StringUtils.hasText(clientIp)) {
            long ipCount = tokenRepository.countByClientIpAndChannelAndCreatedAtAfter(
                    clientIp,
                    channel,
                    now.minusHours(1)
            );
            if (ipCount >= maxRequestsPerIpPerHour) {
                throw new TooManyRequestsException(
                        "Thiết bị/IP đã gửi quá nhiều yêu cầu OTP trong 1 giờ. Vui lòng thử lại sau.",
                        300
                );
            }
        }
    }

    private void invalidateActiveTokens(
            Long userId,
            AuthVerificationToken.Channel channel,
            AuthVerificationToken.Purpose purpose
    ) {
        List<AuthVerificationToken> active = tokenRepository
                .findByUserIdAndChannelAndPurposeAndInvalidatedFalseAndUsedAtIsNull(userId, channel, purpose);
        if (active.isEmpty()) {
            return;
        }
        for (AuthVerificationToken existing : active) {
            existing.setInvalidated(true);
        }
        tokenRepository.saveAll(active);
    }

    private String resolveProviderName(AuthVerificationToken.Channel channel) {
        if (channel == AuthVerificationToken.Channel.EMAIL) {
            return "GMAIL_SMTP";
        }
        return smsOtpProvider != null ? smsOtpProvider.providerName() : "LOCAL_HASH";
    }

    private String generateNumericOtp() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format(Locale.ROOT, "%06d", code);
    }

    private String generateHexTicket() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashSecret(
            AuthVerificationToken.Channel channel,
            AuthVerificationToken.Purpose purpose,
            String normalizedTarget,
            String rawSecret
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = channel.name() + ":" + purpose.name() + ":" + normalizedTarget + ":" + rawSecret + ":" + tokenPepper;
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
