package com.roomcare.backend.auth;

import com.roomcare.backend.dto.AuthResponse;
import com.roomcare.backend.dto.ForgotPasswordRequest;
import com.roomcare.backend.dto.LoginRequest;
import com.roomcare.backend.dto.RegisterRequest;
import com.roomcare.backend.dto.ResendVerificationRequest;
import com.roomcare.backend.dto.ResetPasswordRequest;
import com.roomcare.backend.dto.VerifyEmailRequest;
import com.roomcare.backend.dto.VerifyResetOtpRequest;
import com.roomcare.backend.dto.VerifyResetOtpResponse;
import com.roomcare.backend.entity.AuthVerificationToken;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.Role;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.exception.InvalidCredentialsException;
import com.roomcare.backend.exception.UnverifiedAccountException;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.TenantRepository;
import com.roomcare.backend.repository.UserRepository;
import com.roomcare.backend.security.JwtUtil;
import com.roomcare.backend.service.EmailService;
import com.roomcare.backend.service.EmailVerificationService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service - Business logic cho authentication và xác thực Gmail
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;

    @Value("${roomcare.auth.app-base-url:http://localhost:8080}")
    private String appBaseUrl;

    /**
     * Đăng ký user mới (hỗ trợ LANDLORD và TENANT) ở trạng thái chưa xác thực Gmail.
     * Gửi OTP thật đến Gmail vừa đăng ký; nếu gửi email thất bại thì rollback transaction.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String requestedRole = request.getRole() != null && !request.getRole().isBlank()
                ? request.getRole().trim().toUpperCase(Locale.ROOT)
                : "LANDLORD";
        if (!"LANDLORD".equals(requestedRole) && !"TENANT".equals(requestedRole)) {
            throw new RuntimeException("Vai trò không hợp lệ. Chỉ hỗ trợ Chủ nhà hoặc Người thuê.");
        }
        Role role = Role.valueOf(requestedRole);

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (!normalizedEmail.endsWith("@gmail.com")) {
            throw new RuntimeException("Vui lòng sử dụng địa chỉ Gmail (@gmail.com) đang hoạt động");
        }

        User existingByEmail = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existingByEmail != null) {
            if (Boolean.TRUE.equals(existingByEmail.getEmailVerified())) {
                throw new RuntimeException("Email đã được sử dụng");
            }
            throw new UnverifiedAccountException(
                    "Email này đã đăng ký nhưng chưa xác thực Gmail. Vui lòng nhập mã OTP hoặc gửi lại email xác thực.",
                    existingByEmail.getEmail()
            );
        }

        String normalizedPhone = request.getPhone().trim();
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setIsActive(true);
        user.setEmailVerified(false);
        user.setEmailVerifiedAt(null);
        user.setTokenVersion(0);

        User savedUser = userRepository.save(user);

        if (role == Role.LANDLORD) {
            Property defaultProperty = new Property();
            defaultProperty.setLandlord(savedUser);
            defaultProperty.setName("Nhà trọ " + savedUser.getFullName());
            defaultProperty.setAddress("Chưa cập nhật địa chỉ");
            defaultProperty.setDescription("Nhà trọ mặc định");
            propertyRepository.save(defaultProperty);
        } else {
            tenantRepository.findFirstByEmailIgnoreCaseOrPhone(savedUser.getEmail(), savedUser.getPhone())
                    .ifPresent(tenant -> {
                        if (tenant.getUser() == null) {
                            tenant.setUser(savedUser);
                            tenantRepository.save(tenant);
                        }
                    });
        }

        String otpCode = emailVerificationService.issueOtp(savedUser, AuthVerificationToken.Purpose.EMAIL_VERIFICATION);
        emailService.sendRegistrationVerificationEmail(
                savedUser.getEmail(),
                savedUser.getFullName(),
                otpCode,
                emailVerificationService.getOtpExpirationMinutes(),
                buildVerificationUrl(savedUser.getEmail(), otpCode)
        );

        return AuthResponse.pendingVerification(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                emailVerificationService.getResendCooldownSeconds()
        );
    }

    /**
     * Xác thực Gmail bằng mã OTP 6 số.
     */
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản gắn với email này"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Tài khoản này đã được xác thực trước đó. Bạn có thể đăng nhập ngay.");
        }

        emailVerificationService.verifyEmailOtp(normalizedEmail, request.otp());
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Xác thực Gmail khi người dùng bấm liên kết trong email.
     */
    @Transactional
    public void verifyEmailByLink(String email, String otp) {
        verifyEmail(new VerifyEmailRequest(email, otp));
    }

    /**
     * Gửi lại mã xác thực Gmail (có giới hạn tần suất chống spam).
     */
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) {
            return;
        }
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Tài khoản đã được xác thực. Vui lòng quay lại màn hình đăng nhập.");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        String otpCode = emailVerificationService.issueOtp(user, AuthVerificationToken.Purpose.EMAIL_VERIFICATION);
        emailService.sendRegistrationVerificationEmail(
                user.getEmail(),
                user.getFullName(),
                otpCode,
                emailVerificationService.getOtpExpirationMinutes(),
                buildVerificationUrl(user.getEmail(), otpCode)
        );
    }

    /**
     * Đăng nhập (yêu cầu tài khoản active và đã xác thực Gmail).
     */
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmailOrPhone() != null ? request.getEmailOrPhone().trim() : "";
        User user = userRepository.findByEmailIgnoreCaseOrPhone(identifier, identifier)
                .orElseThrow(() -> new InvalidCredentialsException("Email/Số điện thoại hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Email/Số điện thoại hoặc mật khẩu không đúng");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("Tài khoản đã bị khóa");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new UnverifiedAccountException(
                    "Tài khoản chưa xác thực Gmail. Vui lòng kiểm tra hộp thư hoặc xác thực OTP trước khi đăng nhập.",
                    user.getEmail()
            );
        }

        int tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                tokenVersion
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    /**
     * Khởi tạo yêu cầu quên mật khẩu.
     * Không tiết lộ email có tồn tại hay không nếu email không có trong hệ thống.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            return;
        }

        String otpCode = emailVerificationService.issueOtp(user, AuthVerificationToken.Purpose.PASSWORD_RESET);
        emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getFullName(),
                otpCode,
                emailVerificationService.getOtpExpirationMinutes()
        );
    }

    /**
     * Xác minh mã OTP khôi phục mật khẩu và cấp resetToken dùng 1 lần.
     */
    @Transactional
    public VerifyResetOtpResponse verifyResetOtp(VerifyResetOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String resetTicket = emailVerificationService.verifyResetOtpAndIssueTicket(normalizedEmail, request.otp());
        return new VerifyResetOtpResponse(
                normalizedEmail,
                resetTicket,
                emailVerificationService.getOtpExpirationMinutes()
        );
    }

    /**
     * Đặt lại mật khẩu mới, vô hiệu hóa mã đặt lại và nâng tokenVersion để thu hồi phiên đăng nhập cũ.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!Objects.equals(request.newPassword(), request.confirmPassword())) {
            throw new RuntimeException("Xác nhận mật khẩu mới không khớp");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RuntimeException("Mật khẩu mới tối đa 72 byte");
        }

        String normalizedEmail = normalizeEmail(request.email());
        AuthVerificationToken token = emailVerificationService.consumeResetTicket(normalizedEmail, request.resetToken());
        User user = userRepository.findById(token.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản cần đặt lại mật khẩu"));

        if (!normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Thông tin đặt lại mật khẩu không thuộc tài khoản này");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        int nextVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1;
        user.setTokenVersion(nextVersion);
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
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
