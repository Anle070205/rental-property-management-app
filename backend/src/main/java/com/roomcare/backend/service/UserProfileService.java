package com.roomcare.backend.service;

import com.roomcare.backend.dto.user.ChangePasswordRequest;
import com.roomcare.backend.dto.user.ProfileResponse;
import com.roomcare.backend.dto.user.RequestContactOtpRequest;
import com.roomcare.backend.dto.user.UpdateProfileRequest;
import com.roomcare.backend.dto.user.VerifyContactOtpRequest;
import com.roomcare.backend.entity.AuthVerificationToken;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.UserRepository;
import com.roomcare.backend.util.PhoneNumberUtils;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {
    private static final Pattern GMAIL_PATTERN = Pattern.compile("^(?i)[A-Za-z0-9._%+-]+@gmail\\.com$");

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final OtpVerificationService otpVerificationService;
    private final EmailService emailService;
    private final SmsOtpProvider smsOtpProvider;

    @Autowired
    public UserProfileService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            OtpVerificationService otpVerificationService,
            EmailService emailService,
            SmsOtpProvider smsOtpProvider
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.otpVerificationService = otpVerificationService;
        this.emailService = emailService;
        this.smsOtpProvider = smsOtpProvider;
    }

    public UserProfileService(UserRepository users, PasswordEncoder passwordEncoder) {
        this(users, passwordEncoder, null, null, null);
    }

    public ProfileResponse get(User user) {
        return new ProfileResponse(
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                Boolean.TRUE.equals(user.getPhoneVerified())
        );
    }

    @Transactional
    public ProfileResponse update(User principal, UpdateProfileRequest request) {
        User user = findUser(principal);
        user.setFullName(request.fullName().trim());

        String incomingPhone = request.phone() != null ? request.phone().trim() : "";
        if (!incomingPhone.isBlank()) {
            String normalizedIncoming = PhoneNumberUtils.isValidVietnamMobile(incomingPhone)
                    ? PhoneNumberUtils.normalizeToE164(incomingPhone)
                    : incomingPhone;
            boolean sameAsCurrent = user.getPhone() != null
                    && PhoneNumberUtils.lookupVariants(user.getPhone()).stream()
                    .anyMatch(v -> v.equals(incomingPhone) || v.equals(normalizedIncoming));
            if (!sameAsCurrent) {
                throw new RuntimeException(
                        "Vui lòng xác thực OTP khi bổ sung hoặc thay đổi số điện thoại."
                );
            }
        }
        users.save(user);
        return get(user);
    }

    @Transactional
    public void changePassword(User principal, ChangePasswordRequest request) {
        User user = findUser(principal);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RuntimeException("Mật khẩu mới tối đa 72 byte");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        int nextVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1;
        user.setTokenVersion(nextVersion);
        users.save(user);
    }

    /**
     * Gửi mã OTP để người dùng đã kích hoạt bổ sung & xác thực email hoặc số điện thoại còn lại trong hồ sơ.
     */
    @Transactional
    public void requestContactOtp(User principal, RequestContactOtpRequest request, String clientIp) {
        if (otpVerificationService == null || emailService == null || smsOtpProvider == null) {
            throw new IllegalStateException("Dịch vụ OTP chưa được khởi tạo.");
        }
        User user = findUser(principal);
        AuthVerificationToken.Channel channel = request.channel();

        String normalizedTarget;
        if (channel == AuthVerificationToken.Channel.EMAIL) {
            normalizedTarget = request.target().trim().toLowerCase(Locale.ROOT);
            if (!GMAIL_PATTERN.matcher(normalizedTarget).matches()) {
                throw new RuntimeException("Vui lòng nhập địa chỉ Gmail (@gmail.com) đang hoạt động");
            }
            if (users.findByEmailIgnoreCase(normalizedTarget).filter(other -> !other.getId().equals(user.getId())).isPresent()) {
                throw new RuntimeException("Email đã được sử dụng");
            }
            if (Objects.equals(normalizedTarget, user.getEmail()) && Boolean.TRUE.equals(user.getEmailVerified())) {
                throw new RuntimeException("Gmail này đã được xác thực trên tài khoản của bạn");
            }
        } else {
            if (!PhoneNumberUtils.isValidVietnamMobile(request.target())) {
                throw new RuntimeException("Số điện thoại Việt Nam không hợp lệ (ví dụ: 0912345678 hoặc +84912345678)");
            }
            normalizedTarget = PhoneNumberUtils.normalizeToE164(request.target());
            List<String> variants = PhoneNumberUtils.lookupVariants(normalizedTarget);
            if (users.findFirstByPhoneIn(variants).filter(other -> !other.getId().equals(user.getId())).isPresent()) {
                throw new RuntimeException("Số điện thoại đã được sử dụng");
            }
            if (Objects.equals(normalizedTarget, user.getPhone()) && Boolean.TRUE.equals(user.getPhoneVerified())) {
                throw new RuntimeException("Số điện thoại này đã được xác thực trên tài khoản của bạn");
            }
        }

        OtpVerificationService.IssuedOtp issued = otpVerificationService.createTokenAndDispatchPreparation(
                user, channel, AuthVerificationToken.Purpose.CONTACT_LINK, normalizedTarget, clientIp
        );
        int expMinutes = otpVerificationService.getOtpExpirationMinutes();
        if (channel == AuthVerificationToken.Channel.EMAIL) {
            emailService.sendContactVerificationEmail(normalizedTarget, user.getFullName(), issued.rawOtpOrNull(), expMinutes);
        } else {
            String sid = smsOtpProvider.sendOtp(normalizedTarget, issued.rawOtpOrNull(), "lien ket so dien thoai", expMinutes);
            otpVerificationService.attachProviderReference(issued.token().getId(), sid);
        }
    }

    @Transactional
    public ProfileResponse verifyContactOtp(User principal, VerifyContactOtpRequest request) {
        if (otpVerificationService == null) {
            throw new IllegalStateException("Dịch vụ OTP chưa được khởi tạo.");
        }
        User user = findUser(principal);
        AuthVerificationToken.Channel channel = request.channel();
        String normalizedTarget = channel == AuthVerificationToken.Channel.EMAIL
                ? request.target().trim().toLowerCase(Locale.ROOT)
                : PhoneNumberUtils.normalizeToE164(request.target());

        AuthVerificationToken verifiedToken = otpVerificationService.verifyOtp(
                channel, AuthVerificationToken.Purpose.CONTACT_LINK, normalizedTarget, request.otp()
        );
        if (!Objects.equals(verifiedToken.getUser().getId(), user.getId())) {
            throw new RuntimeException("Mã OTP không thuộc tài khoản đang đăng nhập");
        }

        LocalDateTime now = LocalDateTime.now();
        if (channel == AuthVerificationToken.Channel.EMAIL) {
            if (users.findByEmailIgnoreCase(normalizedTarget).filter(other -> !other.getId().equals(user.getId())).isPresent()) {
                throw new RuntimeException("Email đã được sử dụng");
            }
            user.setEmail(normalizedTarget);
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(now);
        } else {
            List<String> variants = PhoneNumberUtils.lookupVariants(normalizedTarget);
            if (users.findFirstByPhoneIn(variants).filter(other -> !other.getId().equals(user.getId())).isPresent()) {
                throw new RuntimeException("Số điện thoại đã được sử dụng");
            }
            user.setPhone(normalizedTarget);
            user.setPhoneVerified(true);
            user.setPhoneVerifiedAt(now);
        }
        users.save(user);
        return get(user);
    }

    private User findUser(User principal) {
        return users.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    }
}
