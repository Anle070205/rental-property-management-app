package com.roomcare.backend.service;

import com.roomcare.backend.entity.AuthVerificationToken;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.exception.TooManyRequestsException;
import com.roomcare.backend.repository.AuthVerificationTokenRepository;
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

/**
 * Quản lý phát hành, băm SHA-256, chống spam, giới hạn số lần sai và tiêu thụ 1 lần
 * cho OTP xác thực Gmail và đặt lại mật khẩu.
 */
@Service
public class EmailVerificationService {

    private final AuthVerificationTokenRepository tokenRepository;
    private final OtpAttemptService otpAttemptService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int otpExpirationMinutes;
    private final int resendCooldownSeconds;
    private final int maxRequestsPerHour;
    private final int maxAttempts;
    private final String tokenPepper;

    public EmailVerificationService(
            AuthVerificationTokenRepository tokenRepository,
            OtpAttemptService otpAttemptService,
            @Value("${roomcare.auth.otp-expiration-minutes:10}") int otpExpirationMinutes,
            @Value("${roomcare.auth.otp-resend-cooldown-seconds:60}") int resendCooldownSeconds,
            @Value("${roomcare.auth.otp-max-requests-per-hour:5}") int maxRequestsPerHour,
            @Value("${roomcare.auth.otp-max-attempts:5}") int maxAttempts,
            @Value("${roomcare.auth.token-pepper:roomcare-default-pepper}") String tokenPepper
    ) {
        this.tokenRepository = tokenRepository;
        this.otpAttemptService = otpAttemptService;
        this.otpExpirationMinutes = Math.max(1, otpExpirationMinutes);
        this.resendCooldownSeconds = Math.max(1, resendCooldownSeconds);
        this.maxRequestsPerHour = Math.max(1, maxRequestsPerHour);
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
     * Phát hành OTP 6 số mới, vô hiệu hóa các OTP chưa dùng trước đó và kiểm tra giới hạn chống spam.
     */
    @Transactional
    public String issueOtp(User user, AuthVerificationToken.Purpose purpose) {
        String normalizedEmail = normalizeEmail(user.getEmail());
        LocalDateTime now = LocalDateTime.now();

        tokenRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDescIdDesc(normalizedEmail, purpose)
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

        long requestsInLastHour = tokenRepository.countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(
                normalizedEmail,
                purpose,
                now.minusHours(1)
        );
        if (requestsInLastHour >= maxRequestsPerHour) {
            throw new TooManyRequestsException(
                    "Bạn đã vượt quá giới hạn " + maxRequestsPerHour + " lần gửi mã trong 1 giờ. Vui lòng thử lại sau.",
                    300
            );
        }

        invalidateActiveTokens(user.getId(), purpose);

        String rawOtp = generateNumericOtp();
        AuthVerificationToken token = new AuthVerificationToken();
        token.setUser(user);
        token.setEmail(normalizedEmail);
        token.setPurpose(purpose);
        token.setCodeHash(hashSecret(purpose, normalizedEmail, rawOtp));
        token.setExpiresAt(now.plusMinutes(otpExpirationMinutes));
        token.setAttemptCount(0);
        token.setMaxAttempts(maxAttempts);
        token.setInvalidated(false);
        tokenRepository.save(token);

        return rawOtp;
    }

    /**
     * Xác minh OTP đăng ký Gmail và đánh dấu đã sử dụng (chỉ dùng 1 lần).
     */
    @Transactional
    public AuthVerificationToken verifyEmailOtp(String rawEmail, String rawOtp) {
        AuthVerificationToken token = verifyOtpInternal(
                rawEmail,
                rawOtp,
                AuthVerificationToken.Purpose.EMAIL_VERIFICATION
        );
        token.setUsedAt(LocalDateTime.now());
        token.setInvalidated(true);
        return tokenRepository.save(token);
    }

    /**
     * Xác minh OTP đặt lại mật khẩu và cấp ticket 1 lần gắn với bản ghi token đó.
     */
    @Transactional
    public String verifyResetOtpAndIssueTicket(String rawEmail, String rawOtp) {
        AuthVerificationToken token = verifyOtpInternal(
                rawEmail,
                rawOtp,
                AuthVerificationToken.Purpose.PASSWORD_RESET
        );
        String resetTicket = generateHexTicket();
        token.setResetTicketHash(hashSecret(AuthVerificationToken.Purpose.PASSWORD_RESET, token.getEmail(), resetTicket));
        tokenRepository.save(token);
        return resetTicket;
    }

    /**
     * Kiểm tra và tiêu thụ ticket đặt lại mật khẩu (chống dùng lại hoặc đổi chéo email khác).
     */
    @Transactional
    public AuthVerificationToken consumeResetTicket(String rawEmail, String resetTicket) {
        String normalizedEmail = normalizeEmail(rawEmail);
        String expectedTicketHash = hashSecret(
                AuthVerificationToken.Purpose.PASSWORD_RESET,
                normalizedEmail,
                resetTicket != null ? resetTicket.trim() : ""
        );
        AuthVerificationToken token = tokenRepository
                .findTopByEmailIgnoreCaseAndPurposeAndResetTicketHashAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
                        normalizedEmail,
                        AuthVerificationToken.Purpose.PASSWORD_RESET,
                        expectedTicketHash
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
            invalidateActiveTokens(saved.getUser().getId(), AuthVerificationToken.Purpose.PASSWORD_RESET);
        }
        return saved;
    }

    private AuthVerificationToken verifyOtpInternal(
            String rawEmail,
            String rawOtp,
            AuthVerificationToken.Purpose purpose
    ) {
        String normalizedEmail = normalizeEmail(rawEmail);
        String cleanOtp = rawOtp != null ? rawOtp.trim() : "";

        AuthVerificationToken token = tokenRepository
                .findTopByEmailIgnoreCaseAndPurposeAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
                        normalizedEmail,
                        purpose
                )
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy mã xác thực còn hiệu lực hoặc mã đã được sử dụng. Vui lòng yêu cầu mã mới."
                ));

        LocalDateTime now = LocalDateTime.now();
        if (token.getExpiresAt().isBefore(now)) {
            throw new RuntimeException("Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
        }

        int currentAttempts = token.getAttemptCount() != null ? token.getAttemptCount() : 0;
        int limit = token.getMaxAttempts() != null && token.getMaxAttempts() > 0 ? token.getMaxAttempts() : maxAttempts;
        if (currentAttempts >= limit) {
            throw new RuntimeException("Mã OTP đã bị vô hiệu hóa do nhập sai quá nhiều lần. Vui lòng gửi lại mã mới.");
        }

        String candidateHash = hashSecret(purpose, normalizedEmail, cleanOtp);
        boolean matches = constantTimeEquals(token.getCodeHash(), candidateHash);
        if (!matches) {
            int remaining = otpAttemptService.recordFailedAttempt(token.getId());
            if (remaining <= 0) {
                throw new RuntimeException("Bạn đã nhập sai OTP quá " + limit + " lần. Vui lòng yêu cầu mã mới.");
            }
            throw new RuntimeException("Mã OTP không chính xác. Bạn còn " + remaining + " lần thử.");
        }

        return token;
    }

    private void invalidateActiveTokens(Long userId, AuthVerificationToken.Purpose purpose) {
        List<AuthVerificationToken> active = tokenRepository
                .findByUserIdAndPurposeAndInvalidatedFalseAndUsedAtIsNull(userId, purpose);
        if (active.isEmpty()) {
            return;
        }
        for (AuthVerificationToken existing : active) {
            existing.setInvalidated(true);
        }
        tokenRepository.saveAll(active);
    }

    public String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
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

    private String hashSecret(AuthVerificationToken.Purpose purpose, String normalizedEmail, String rawSecret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = purpose.name() + ":" + normalizedEmail + ":" + rawSecret + ":" + tokenPepper;
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
