package com.roomcare.backend.auth;

import com.roomcare.backend.dto.AuthResponse;
import com.roomcare.backend.dto.ForgotPasswordRequest;
import com.roomcare.backend.dto.LoginRequest;
import com.roomcare.backend.dto.RegisterRequest;
import com.roomcare.backend.dto.ResendOtpRequest;
import com.roomcare.backend.dto.ResetPasswordRequest;
import com.roomcare.backend.dto.VerifyOtpRequest;
import com.roomcare.backend.dto.VerifyResetOtpRequest;
import com.roomcare.backend.dto.VerifyResetOtpResponse;
import com.roomcare.backend.entity.AuthVerificationToken;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.Role;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.exception.InvalidCredentialsException;
import com.roomcare.backend.exception.UnverifiedAccountException;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.TenantRepository;
import com.roomcare.backend.repository.UserRepository;
import com.roomcare.backend.security.JwtUtil;
import com.roomcare.backend.service.EmailService;
import com.roomcare.backend.service.OtpVerificationService;
import com.roomcare.backend.service.SmsOtpProvider;
import com.roomcare.backend.util.PhoneNumberUtils;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service - Xác thực Email (Gmail) hoặc Số điện thoại (+84) qua OTP thật,
 * đăng nhập, khôi phục mật khẩu và thu hồi JWT theo tokenVersion.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern GMAIL_PATTERN = Pattern.compile("^(?i)[A-Za-z0-9._%+-]+@gmail\\.com$");

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpVerificationService otpVerificationService;
    private final EmailService emailService;
    private final SmsOtpProvider smsOtpProvider;

    @Value("${roomcare.auth.app-base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return register(request, null);
    }

    /**
     * Đăng ký bằng Gmail HOẶC Số điện thoại Việt Nam (chọn 1 trong 2 phương thức).
     * Chỉ kích hoạt và cấp JWT sau khi xác minh OTP thành công.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, String clientIp) {
        String requestedRole = request.getRole() != null && !request.getRole().isBlank()
                ? request.getRole().trim().toUpperCase(Locale.ROOT)
                : "LANDLORD";
        if (!"LANDLORD".equals(requestedRole) && !"TENANT".equals(requestedRole)) {
            throw new RuntimeException("Vai trò không hợp lệ. Chỉ hỗ trợ Chủ nhà hoặc Người thuê.");
        }
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RuntimeException("Mật khẩu tối đa 72 byte");
        }
        Role role = Role.valueOf(requestedRole);
        AuthVerificationToken.Channel channel = parseChannel(request.getVerificationMethod());

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setIsActive(true);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setTokenVersion(0);

        String normalizedTarget;
        if (channel == AuthVerificationToken.Channel.EMAIL) {
            normalizedTarget = validateAndNormalizeGmail(request.getEmail());
            Optional<User> existingOpt = userRepository.findByEmailIgnoreCase(normalizedTarget);
            if (existingOpt.isPresent()) {
                User existing = existingOpt.get();
                if (existing.isAccountVerified()) {
                    throw new RuntimeException("Email đã được sử dụng");
                }
                throw new UnverifiedAccountException(
                        "Email này đã đăng ký nhưng chưa xác thực Gmail. Vui lòng nhập OTP hoặc gửi lại mã.",
                        AuthVerificationToken.Channel.EMAIL,
                        existing.getEmail()
                );
            }
            user.setEmail(normalizedTarget);
            user.setPhone(null);
        } else {
            normalizedTarget = validateAndNormalizeVietnamPhone(request.getPhone());
            List<String> variants = PhoneNumberUtils.lookupVariants(normalizedTarget);
            Optional<User> existingOpt = userRepository.findFirstByPhoneIn(variants);
            if (existingOpt.isPresent()) {
                User existing = existingOpt.get();
                if (existing.isAccountVerified()) {
                    throw new RuntimeException("Số điện thoại đã được sử dụng");
                }
                throw new UnverifiedAccountException(
                        "Số điện thoại này đã đăng ký nhưng chưa xác thực OTP. Vui lòng nhập OTP hoặc gửi lại mã.",
                        AuthVerificationToken.Channel.PHONE,
                        existing.getPhone()
                );
            }
            user.setPhone(normalizedTarget);
            user.setEmail(null);
        }

        User savedUser = userRepository.save(user);

        if (role == Role.LANDLORD) {
            Property defaultProperty = new Property();
            defaultProperty.setLandlord(savedUser);
            defaultProperty.setName("Nhà trọ " + savedUser.getFullName());
            defaultProperty.setAddress("Chưa cập nhật địa chỉ");
            defaultProperty.setDescription("Nhà trọ mặc định");
            propertyRepository.save(defaultProperty);
        }

        dispatchOtp(
                savedUser,
                channel,
                AuthVerificationToken.Purpose.REGISTRATION,
                normalizedTarget,
                clientIp
        );

        return AuthResponse.pendingVerification(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole().name(),
                channel,
                normalizedTarget,
                otpVerificationService.getResendCooldownSeconds()
        );
    }

    /**
     * Xác minh OTP đăng ký (EMAIL hoặc PHONE) và kích hoạt tài khoản.
     */
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        AuthVerificationToken.Channel channel = request.channel();
        String normalizedTarget = normalizeContactForChannel(channel, request.target());
        User user = findUserByChannelTarget(channel, normalizedTarget)
                .orElseThrow(() -> new RuntimeException(
                        channel == AuthVerificationToken.Channel.EMAIL
                                ? "Không tìm thấy tài khoản gắn với email này"
                                : "Không tìm thấy tài khoản gắn với số điện thoại này"
                ));

        boolean alreadyVerified = channel == AuthVerificationToken.Channel.EMAIL
                ? Boolean.TRUE.equals(user.getEmailVerified())
                : Boolean.TRUE.equals(user.getPhoneVerified());
        if (alreadyVerified) {
            throw new RuntimeException("Tài khoản đã được xác thực trước đó. Bạn có thể đăng nhập ngay.");
        }

        otpVerificationService.verifyOtp(
                channel,
                AuthVerificationToken.Purpose.REGISTRATION,
                normalizedTarget,
                request.otp()
        );

        LocalDateTime now = LocalDateTime.now();
        if (channel == AuthVerificationToken.Channel.EMAIL) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(now);
        } else {
            user.setPhoneVerified(true);
            user.setPhoneVerifiedAt(now);
        }

        if (user.getRole() == Role.TENANT) {
            linkMatchingTenantRecord(user, channel);
        }
        userRepository.save(user);
    }

    @Transactional
    public void verifyEmailByLink(String email, String otp) {
        verifyOtp(new VerifyOtpRequest(AuthVerificationToken.Channel.EMAIL, email, otp));
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request, String clientIp) {
        AuthVerificationToken.Channel channel = request.channel();
        String normalizedTarget = normalizeContactForChannel(channel, request.target());
        User user = findUserByChannelTarget(channel, normalizedTarget).orElse(null);
        if (user == null) {
            return;
        }
        boolean alreadyVerified = channel == AuthVerificationToken.Channel.EMAIL
                ? Boolean.TRUE.equals(user.getEmailVerified())
                : Boolean.TRUE.equals(user.getPhoneVerified());
        if (alreadyVerified) {
            throw new RuntimeException("Phương thức này đã được xác thực. Vui lòng quay lại đăng nhập.");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        dispatchOtp(
                user,
                channel,
                AuthVerificationToken.Purpose.REGISTRATION,
                normalizedTarget,
                clientIp
        );
    }

    /**
     * Đăng nhập bằng Email đã xác thực hoặc Số điện thoại (0... / +84...) đã xác thực.
     */
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmailOrPhone() != null ? request.getEmailOrPhone().trim() : "";
        boolean isEmailLogin = identifier.contains("@");

        User user;
        if (isEmailLogin) {
            user = userRepository.findByEmailIgnoreCase(identifier)
                    .orElseThrow(() -> new InvalidCredentialsException("Email/Số điện thoại hoặc mật khẩu không đúng"));
        } else {
            List<String> phoneVariants = PhoneNumberUtils.lookupVariants(identifier);
            user = userRepository.findFirstByPhoneIn(phoneVariants)
                    .orElseThrow(() -> new InvalidCredentialsException("Email/Số điện thoại hoặc mật khẩu không đúng"));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Email/Số điện thoại hoặc mật khẩu không đúng");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("Tài khoản đã bị khóa");
        }

        if (isEmailLogin && !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new UnverifiedAccountException(
                    "Tài khoản chưa xác thực Gmail. Vui lòng kiểm tra hộp thư hoặc xác thực OTP trước khi đăng nhập.",
                    AuthVerificationToken.Channel.EMAIL,
                    user.getEmail()
            );
        }
        if (!isEmailLogin && !Boolean.TRUE.equals(user.getPhoneVerified())) {
            throw new UnverifiedAccountException(
                    "Số điện thoại chưa xác thực OTP. Vui lòng xác thực SMS OTP trước khi đăng nhập.",
                    AuthVerificationToken.Channel.PHONE,
                    user.getPhone()
            );
        }
        if (!user.isAccountVerified()) {
            AuthVerificationToken.Channel ch = user.getEmail() != null
                    ? AuthVerificationToken.Channel.EMAIL
                    : AuthVerificationToken.Channel.PHONE;
            throw new UnverifiedAccountException(
                    "Tài khoản chưa được kích hoạt. Vui lòng hoàn tất xác thực OTP.",
                    ch,
                    user.getPrimaryIdentifier()
            );
        }

        int tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getPrimaryIdentifier(),
                user.getRole().name(),
                tokenVersion
        );

        return AuthResponse.verifiedLogin(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                Boolean.TRUE.equals(user.getPhoneVerified())
        );
    }

    /**
     * Quên mật khẩu qua Gmail hoặc Số điện thoại.
     * Không tiết lộ tài khoản có tồn tại hay không nếu không tìm thấy.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, String clientIp) {
        AuthVerificationToken.Channel channel = request.channel();
        String normalizedTarget = normalizeContactForChannel(channel, request.target());

        User user = findUserByChannelTarget(channel, normalizedTarget).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            return;
        }

        dispatchOtp(
                user,
                channel,
                AuthVerificationToken.Purpose.PASSWORD_RESET,
                normalizedTarget,
                clientIp
        );
    }

    @Transactional
    public VerifyResetOtpResponse verifyResetOtp(VerifyResetOtpRequest request) {
        AuthVerificationToken.Channel channel = request.channel();
        String normalizedTarget = normalizeContactForChannel(channel, request.target());
        String resetTicket = otpVerificationService.verifyResetOtpAndIssueTicket(
                channel,
                normalizedTarget,
                request.otp()
        );
        return new VerifyResetOtpResponse(
                channel.name(),
                normalizedTarget,
                resetTicket,
                otpVerificationService.getOtpExpirationMinutes()
        );
    }

    /**
     * Đặt lại mật khẩu mới, tăng tokenVersion vô hiệu hóa JWT cũ và hủy toàn bộ token reset.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!Objects.equals(request.newPassword(), request.confirmPassword())) {
            throw new RuntimeException("Xác nhận mật khẩu mới không khớp");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RuntimeException("Mật khẩu mới tối đa 72 byte");
        }

        AuthVerificationToken.Channel channel = request.channel();
        String normalizedTarget = normalizeContactForChannel(channel, request.target());
        AuthVerificationToken token = otpVerificationService.consumeResetTicket(
                channel,
                normalizedTarget,
                request.resetToken()
        );

        User user = userRepository.findById(token.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản cần đặt lại mật khẩu"));

        boolean ownsTarget = switch (channel) {
            case EMAIL -> user.getEmail() != null && normalizedTarget.equalsIgnoreCase(user.getEmail());
            case PHONE -> user.getPhone() != null
                    && PhoneNumberUtils.lookupVariants(normalizedTarget).contains(user.getPhone());
        };
        if (!ownsTarget) {
            throw new RuntimeException("Thông tin đặt lại mật khẩu không thuộc tài khoản này");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        int nextVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1;
        user.setTokenVersion(nextVersion);

        LocalDateTime now = LocalDateTime.now();
        if (channel == AuthVerificationToken.Channel.EMAIL && !Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(now);
        } else if (channel == AuthVerificationToken.Channel.PHONE && !Boolean.TRUE.equals(user.getPhoneVerified())) {
            user.setPhoneVerified(true);
            user.setPhoneVerifiedAt(now);
        }
        userRepository.save(user);
    }

    public void dispatchOtp(
            User user,
            AuthVerificationToken.Channel channel,
            AuthVerificationToken.Purpose purpose,
            String normalizedTarget,
            String clientIp
    ) {
        OtpVerificationService.IssuedOtp issued = otpVerificationService.createTokenAndDispatchPreparation(
                user, channel, purpose, normalizedTarget, clientIp
        );
        int expMinutes = otpVerificationService.getOtpExpirationMinutes();

        if (channel == AuthVerificationToken.Channel.EMAIL) {
            String otpCode = issued.rawOtpOrNull();
            switch (purpose) {
                case REGISTRATION -> emailService.sendRegistrationVerificationEmail(
                        normalizedTarget,
                        user.getFullName(),
                        otpCode,
                        expMinutes,
                        buildVerificationUrl(normalizedTarget, otpCode)
                );
                case PASSWORD_RESET -> emailService.sendPasswordResetEmail(
                        normalizedTarget,
                        user.getFullName(),
                        otpCode,
                        expMinutes
                );
                case CONTACT_LINK -> emailService.sendContactVerificationEmail(
                        normalizedTarget,
                        user.getFullName(),
                        otpCode,
                        expMinutes
                );
            }
        } else {
            String purposeLabel = switch (purpose) {
                case REGISTRATION -> "dang ky tai khoan";
                case PASSWORD_RESET -> "dat lai mat khau";
                case CONTACT_LINK -> "xac thuc so dien thoai";
            };
            String sid = smsOtpProvider.sendOtp(normalizedTarget, issued.rawOtpOrNull(), purposeLabel, expMinutes);
            otpVerificationService.attachProviderReference(issued.token().getId(), sid);
        }
    }

    private void linkMatchingTenantRecord(User user, AuthVerificationToken.Channel channel) {
        Optional<Tenant> matched = channel == AuthVerificationToken.Channel.EMAIL
                ? tenantRepository.findFirstByEmailIgnoreCaseOrPhone(user.getEmail(), null)
                : tenantRepository.findFirstByPhoneIn(PhoneNumberUtils.lookupVariants(user.getPhone()));
        matched.ifPresent(tenant -> {
            if (tenant.getUser() == null) {
                tenant.setUser(user);
                tenantRepository.save(tenant);
            }
        });
    }

    private Optional<User> findUserByChannelTarget(AuthVerificationToken.Channel channel, String normalizedTarget) {
        return switch (channel) {
            case EMAIL -> userRepository.findByEmailIgnoreCase(normalizedTarget);
            case PHONE -> userRepository.findFirstByPhoneIn(PhoneNumberUtils.lookupVariants(normalizedTarget));
        };
    }

    private AuthVerificationToken.Channel parseChannel(String rawMethod) {
        if (rawMethod == null || rawMethod.isBlank()) {
            return AuthVerificationToken.Channel.EMAIL;
        }
        try {
            return AuthVerificationToken.Channel.valueOf(rawMethod.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Phương thức xác thực không hợp lệ (chỉ hỗ trợ EMAIL hoặc PHONE)");
        }
    }

    private String normalizeContactForChannel(AuthVerificationToken.Channel channel, String rawTarget) {
        return switch (channel) {
            case EMAIL -> validateAndNormalizeGmailForChannel(rawTarget);
            case PHONE -> validateAndNormalizeVietnamPhone(rawTarget);
        };
    }

    private String validateAndNormalizeGmail(String email) {
        String clean = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        if (clean.isBlank()) {
            throw new RuntimeException("Địa chỉ Gmail không được để trống");
        }
        if (!GMAIL_PATTERN.matcher(clean).matches()) {
            throw new RuntimeException("Vui lòng nhập địa chỉ Gmail (@gmail.com) đang hoạt động");
        }
        return clean;
    }

    private String validateAndNormalizeGmailForChannel(String email) {
        String clean = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        if (clean.isBlank() || !clean.contains("@")) {
            throw new RuntimeException("Email không hợp lệ");
        }
        return clean;
    }

    private String validateAndNormalizeVietnamPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }
        if (!PhoneNumberUtils.isValidVietnamMobile(phone)) {
            throw new RuntimeException("Số điện thoại Việt Nam không hợp lệ (ví dụ: 0912345678 hoặc +84912345678)");
        }
        return PhoneNumberUtils.normalizeToE164(phone);
    }

    private String buildVerificationUrl(String email, String otpCode) {
        String base = appBaseUrl != null && !appBaseUrl.isBlank()
                ? appBaseUrl.replaceAll("/+$", "")
                : "http://localhost:8080";
        return base + "/api/auth/verify-email?email="
                + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&otp=" + URLEncoder.encode(otpCode, StandardCharsets.UTF_8);
    }
}
