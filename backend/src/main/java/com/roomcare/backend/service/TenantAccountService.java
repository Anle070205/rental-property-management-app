package com.roomcare.backend.service;

import com.roomcare.backend.dto.ActivateAccountRequest;
import com.roomcare.backend.dto.tenant.ProvisionTenantAccountRequest;
import com.roomcare.backend.dto.tenant.ProvisionTenantAccountResponse;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.ContractRepository;
import com.roomcare.backend.repository.TenantRepository;
import com.roomcare.backend.repository.UserRepository;
import com.roomcare.backend.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Dịch vụ do Chủ trọ khởi tạo/liên kết tài khoản Khách thuê (TENANT) và kích hoạt 1 lần qua OTP.
 * Không tạo tài khoản trùng và tuyệt đối không gửi mật khẩu plaintext qua Email/SMS.
 */
@Service
@RequiredArgsConstructor
public class TenantAccountService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpVerificationService otpVerificationService;
    private final EmailService emailService;
    private final SmsOtpProvider smsOtpProvider;

    @Autowired(required = false)
    private ContractRepository contractRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ProvisionTenantAccountResponse provisionOrLinkAccount(
            Long landlordId,
            Long tenantId,
            ProvisionTenantAccountRequest request,
            String clientIp) {

        Tenant tenant = findOwnedTenant(landlordId, tenantId);
        AuthVerificationToken.Channel channel = request.getChannel();
        String rawTarget = (request.getTarget() != null && !request.getTarget().isBlank())
                ? request.getTarget().trim()
                : (channel == AuthVerificationToken.Channel.EMAIL ? tenant.getEmail() : tenant.getPhone());

        String normalizedTarget = validateAndNormalizeTarget(channel, rawTarget);

        if (tenant.getUser() != null && tenant.getUser().isAccountVerified()) {
            return new ProvisionTenantAccountResponse(
                    tenant.getId(),
                    tenant.getUser().getId(),
                    "ACTIVE",
                    "ALREADY_ACTIVE",
                    channel.name(),
                    normalizedTarget,
                    0,
                    "Khách thuê đã có tài khoản đang hoạt động."
            );
        }

        Optional<User> existingUserOpt = findUserByTarget(channel, normalizedTarget);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getRole() != Role.TENANT) {
                throw new RuntimeException("Email/Số điện thoại này đã thuộc tài khoản Chủ nhà, không thể cấp cho Khách thuê");
            }
            Optional<Tenant> boundTenant = tenantRepository.findByUserId(existingUser.getId());
            if (boundTenant.isPresent() && !Objects.equals(boundTenant.get().getId(), tenant.getId())) {
                throw new RuntimeException("Tài khoản này đã được liên kết với hồ sơ khách thuê khác");
            }

            if (existingUser.isAccountVerified()) {
                if (!Boolean.TRUE.equals(request.getConfirmLinkExisting())) {
                    throw new RuntimeException("Tài khoản đã tồn tại trên RoomCare. Vui lòng xác nhận gửi OTP xác thực quyền sở hữu để liên kết.");
                }
                syncTenantContactFromTarget(tenant, channel, normalizedTarget);
                tenantRepository.save(tenant);
                dispatchActivationOtp(tenant, existingUser, channel, normalizedTarget, clientIp);
                return new ProvisionTenantAccountResponse(
                        tenant.getId(),
                        existingUser.getId(),
                        "PENDING_ACTIVATION",
                        "LINK_OTP_SENT",
                        channel.name(),
                        normalizedTarget,
                        otpVerificationService.getResendCooldownSeconds(),
                        "Tài khoản đã tồn tại. Hệ thống đã gửi mã OTP xác nhận quyền sở hữu tới " + normalizedTarget + " để hoàn tất liên kết."
                );
            }

            // Tai khoan chua kich hoat truoc do -> gan vao tenant hien tai (neu chua gan) va gui OTP
            tenant.setUser(existingUser);
            syncTenantContactFromTarget(tenant, channel, normalizedTarget);
            tenantRepository.save(tenant);
            dispatchActivationOtp(tenant, existingUser, channel, normalizedTarget, clientIp);
            return new ProvisionTenantAccountResponse(
                    tenant.getId(),
                    existingUser.getId(),
                    "PENDING_ACTIVATION",
                    "CREATED_PENDING_ACTIVATION",
                    channel.name(),
                    normalizedTarget,
                    otpVerificationService.getResendCooldownSeconds(),
                    "Đã gửi mã OTP kích hoạt và hướng dẫn đặt mật khẩu riêng tới " + normalizedTarget + "."
            );
        }

        // Tao tai khoan TENANT moi o trang thai cho kich hoat, khong tao mat khau plaintext
        User newUser = new User();
        newUser.setFullName(tenant.getFullName());
        newUser.setRole(Role.TENANT);
        newUser.setIsActive(true);
        newUser.setEmailVerified(false);
        newUser.setPhoneVerified(false);
        newUser.setTokenVersion(0);
        if (channel == AuthVerificationToken.Channel.EMAIL) {
            newUser.setEmail(normalizedTarget);
            newUser.setPhone(null);
        } else {
            newUser.setPhone(normalizedTarget);
            newUser.setEmail(null);
        }
        newUser.setPassword(passwordEncoder.encode(generateRandomUnusableSecret()));

        User savedUser = userRepository.save(newUser);
        tenant.setUser(savedUser);
        syncTenantContactFromTarget(tenant, channel, normalizedTarget);
        tenantRepository.save(tenant);

        dispatchActivationOtp(tenant, savedUser, channel, normalizedTarget, clientIp);
        return new ProvisionTenantAccountResponse(
                tenant.getId(),
                savedUser.getId(),
                "PENDING_ACTIVATION",
                "CREATED_PENDING_ACTIVATION",
                channel.name(),
                normalizedTarget,
                otpVerificationService.getResendCooldownSeconds(),
                "Đã tạo tài khoản khách thuê và gửi mã OTP kích hoạt tới " + normalizedTarget + "."
        );
    }

    @Transactional
    public ProvisionTenantAccountResponse resendActivationOtp(
            Long landlordId,
            Long tenantId,
            AuthVerificationToken.Channel requestedChannel,
            String clientIp) {
        Tenant tenant = findOwnedTenant(landlordId, tenantId);
        if (tenant.getUser() != null && tenant.getUser().isAccountVerified()) {
            throw new RuntimeException("Tài khoản khách thuê đã được kích hoạt.");
        }

        AuthVerificationToken.Channel channel = requestedChannel;
        if (channel == null) {
            if (tenant.getUser() != null && tenant.getUser().getEmail() != null) {
                channel = AuthVerificationToken.Channel.EMAIL;
            } else if (tenant.getUser() != null && tenant.getUser().getPhone() != null) {
                channel = AuthVerificationToken.Channel.PHONE;
            } else if (tenant.getEmail() != null && !tenant.getEmail().isBlank()) {
                channel = AuthVerificationToken.Channel.EMAIL;
            } else {
                channel = AuthVerificationToken.Channel.PHONE;
            }
        }

        String rawTarget = tenant.getUser() != null
                ? (channel == AuthVerificationToken.Channel.EMAIL ? tenant.getUser().getEmail() : tenant.getUser().getPhone())
                : (channel == AuthVerificationToken.Channel.EMAIL ? tenant.getEmail() : tenant.getPhone());
        String normalizedTarget = validateAndNormalizeTarget(channel, rawTarget);

        User targetUser = tenant.getUser() != null
                ? tenant.getUser()
                : findUserByTarget(channel, normalizedTarget)
                .orElseThrow(() -> new RuntimeException("Chưa khởi tạo yêu cầu cấp tài khoản cho khách thuê này."));

        dispatchActivationOtp(tenant, targetUser, channel, normalizedTarget, clientIp);
        return new ProvisionTenantAccountResponse(
                tenant.getId(),
                targetUser.getId(),
                "PENDING_ACTIVATION",
                "LINK_OTP_SENT",
                channel.name(),
                normalizedTarget,
                otpVerificationService.getResendCooldownSeconds(),
                "Đã gửi lại mã OTP kích hoạt tới " + normalizedTarget + "."
        );
    }

    @Transactional
    public void activateAccount(ActivateAccountRequest request) {
        if (!Objects.equals(request.newPassword(), request.confirmPassword())) {
            throw new RuntimeException("Xác nhận mật khẩu mới không khớp");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RuntimeException("Mật khẩu mới tối đa 72 byte");
        }

        AuthVerificationToken.Channel channel = request.channel();
        String normalizedTarget = validateAndNormalizeTarget(channel, request.target());

        AuthVerificationToken verifiedToken = otpVerificationService.verifyOtp(
                channel,
                AuthVerificationToken.Purpose.ACCOUNT_ACTIVATION,
                normalizedTarget,
                request.otp()
        );

        User user = userRepository.findById(verifiedToken.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản cần kích hoạt"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }
        if (user.getRole() != Role.TENANT) {
            throw new RuntimeException("Chỉ tài khoản Khách thuê mới kích hoạt qua luồng này");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        int nextVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1;
        user.setTokenVersion(nextVersion);
        if (channel == AuthVerificationToken.Channel.EMAIL) {
            user.setEmail(normalizedTarget);
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(now);
        } else {
            user.setPhone(normalizedTarget);
            user.setPhoneVerified(true);
            user.setPhoneVerifiedAt(now);
        }
        User savedUser = userRepository.save(user);

        // Lien ket ho so Tenant tuong ung neu dang lien ket tai khoan da ton tai
        Optional<Tenant> byUser = tenantRepository.findByUserId(savedUser.getId());
        if (byUser.isEmpty()) {
            Optional<Tenant> matchingTenant = channel == AuthVerificationToken.Channel.EMAIL
                    ? tenantRepository.findFirstByEmailIgnoreCaseOrPhone(normalizedTarget, null)
                    : tenantRepository.findFirstByPhoneIn(PhoneNumberUtils.lookupVariants(normalizedTarget));
            matchingTenant.ifPresent(t -> {
                if (t.getUser() == null) {
                    t.setUser(savedUser);
                    Tenant linked = tenantRepository.save(t);
                    notifyActivation(savedUser, linked.getLandlord());
                }
            });
        } else {
            notifyActivation(savedUser, byUser.get().getLandlord());
        }
    }

    private void notifyActivation(User tenantUser, User landlord) {
        if (notificationService == null) {
            return;
        }
        notificationService.createNotification(
                tenantUser,
                NotificationType.SYSTEM,
                "Kích hoạt tài khoản thành công",
                "Tài khoản RoomCare của bạn đã được kích hoạt. Bạn có thể xem phòng thuê, hợp đồng và hóa đơn hàng tháng.",
                tenantUser.getId()
        );
        if (landlord != null) {
            notificationService.createNotification(
                    landlord,
                    NotificationType.SYSTEM,
                    "Khách thuê đã kích hoạt tài khoản",
                    "Khách thuê " + tenantUser.getFullName() + " đã kích hoạt tài khoản và thiết lập mật khẩu riêng.",
                    tenantUser.getId()
            );
        }
    }

    private void dispatchActivationOtp(
            Tenant tenant,
            User targetUser,
            AuthVerificationToken.Channel channel,
            String normalizedTarget,
            String clientIp) {
        OtpVerificationService.IssuedOtp issued = otpVerificationService.createTokenAndDispatchPreparation(
                targetUser,
                channel,
                AuthVerificationToken.Purpose.ACCOUNT_ACTIVATION,
                normalizedTarget,
                clientIp
        );
        int expMinutes = otpVerificationService.getOtpExpirationMinutes();
        if (channel == AuthVerificationToken.Channel.EMAIL) {
            String roomCode = resolveRoomCode(tenant);
            emailService.sendAccountActivationEmail(
                    normalizedTarget,
                    tenant.getFullName(),
                    tenant.getLandlord().getFullName(),
                    roomCode,
                    issued.rawOtpOrNull(),
                    expMinutes
            );
        } else {
            String sid = smsOtpProvider.sendOtp(
                    normalizedTarget,
                    issued.rawOtpOrNull(),
                    "kich hoat tai khoan khach thue",
                    expMinutes
            );
            otpVerificationService.attachProviderReference(issued.token().getId(), sid);
        }
    }

    private String resolveRoomCode(Tenant tenant) {
        if (contractRepository == null || tenant == null) {
            return "";
        }
        return contractRepository
                .findFirstByTenantIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(
                        tenant.getId(), List.of(ContractStatus.ACTIVE, ContractStatus.SIGNED))
                .map(c -> c.getRoom().getRoomCode())
                .orElse("");
    }

    private void syncTenantContactFromTarget(Tenant tenant, AuthVerificationToken.Channel channel, String target) {
        if (channel == AuthVerificationToken.Channel.EMAIL && (tenant.getEmail() == null || tenant.getEmail().isBlank())) {
            tenant.setEmail(target);
        } else if (channel == AuthVerificationToken.Channel.PHONE && (tenant.getPhone() == null || tenant.getPhone().isBlank())) {
            tenant.setPhone(target);
        }
    }

    private Tenant findOwnedTenant(Long landlordId, Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ khách thuê"));
        if (!tenant.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền cấp tài khoản cho khách thuê này");
        }
        return tenant;
    }

    private Optional<User> findUserByTarget(AuthVerificationToken.Channel channel, String normalizedTarget) {
        return switch (channel) {
            case EMAIL -> userRepository.findByEmailIgnoreCase(normalizedTarget);
            case PHONE -> userRepository.findFirstByPhoneIn(PhoneNumberUtils.lookupVariants(normalizedTarget));
        };
    }

    private String validateAndNormalizeTarget(AuthVerificationToken.Channel channel, String rawTarget) {
        String trimmed = rawTarget != null ? rawTarget.trim() : "";
        if (trimmed.isBlank()) {
            throw new RuntimeException(channel == AuthVerificationToken.Channel.EMAIL
                    ? "Khách thuê chưa có địa chỉ Email để gửi mã kích hoạt"
                    : "Khách thuê chưa có số điện thoại Việt Nam (+84) để gửi mã kích hoạt");
        }
        if (channel == AuthVerificationToken.Channel.EMAIL) {
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (!EMAIL_PATTERN.matcher(lower).matches()) {
                throw new RuntimeException("Địa chỉ Email không hợp lệ");
            }
            return lower;
        }
        return PhoneNumberUtils.normalizeToE164(trimmed);
    }

    private String generateRandomUnusableSecret() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
