package com.roomcare.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomcare.backend.auth.AuthController;
import com.roomcare.backend.auth.AuthService;
import com.roomcare.backend.dto.AuthResponse;
import com.roomcare.backend.dto.ForgotPasswordRequest;
import com.roomcare.backend.dto.LoginRequest;
import com.roomcare.backend.dto.RegisterRequest;
import com.roomcare.backend.dto.ResendVerificationRequest;
import com.roomcare.backend.dto.ResetPasswordRequest;
import com.roomcare.backend.dto.VerifyEmailRequest;
import com.roomcare.backend.dto.VerifyResetOtpRequest;
import com.roomcare.backend.dto.VerifyResetOtpResponse;
import com.roomcare.backend.dto.user.ChangePasswordRequest;
import com.roomcare.backend.entity.AuthVerificationToken;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.Role;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.exception.GlobalExceptionHandler;
import com.roomcare.backend.exception.InvalidCredentialsException;
import com.roomcare.backend.exception.MailDeliveryException;
import com.roomcare.backend.exception.TooManyRequestsException;
import com.roomcare.backend.exception.UnverifiedAccountException;
import com.roomcare.backend.repository.AuthVerificationTokenRepository;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.TenantRepository;
import com.roomcare.backend.repository.UserRepository;
import com.roomcare.backend.security.JwtUtil;
import com.roomcare.backend.service.EmailService;
import com.roomcare.backend.service.EmailVerificationService;
import com.roomcare.backend.service.GmailEmailService;
import com.roomcare.backend.service.OtpAttemptService;
import com.roomcare.backend.service.UserProfileService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GmailAuthVerificationResetTest {

    @Mock private UserRepository userRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private AuthVerificationTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;

    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private EmailVerificationService emailVerificationService;
    private AuthService authService;
    private AuthController authController;
    private UserProfileService userProfileService;

    private final Map<Long, User> userStore = new LinkedHashMap<>();
    private final Map<Long, AuthVerificationToken> tokenStore = new LinkedHashMap<>();
    private final AtomicLong userSeq = new AtomicLong(1);
    private final AtomicLong tokenSeq = new AtomicLong(1);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userStore.clear();
        tokenStore.clear();
        userSeq.set(1);
        tokenSeq.set(1);

        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(
                jwtUtil,
                "secret",
                "RoomCareSecretKeyForJWTTokenGenerationAndValidation2024VeryLongSecretKey"
        );
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86_400_000L);

        wireRepositories();

        OtpAttemptService otpAttemptService = new OtpAttemptService(tokenRepository);
        emailVerificationService = new EmailVerificationService(
                tokenRepository,
                otpAttemptService,
                10,
                60,
                5,
                5,
                "unit-test-pepper"
        );
        authService = new AuthService(
                userRepository,
                propertyRepository,
                tenantRepository,
                passwordEncoder,
                jwtUtil,
                emailVerificationService,
                emailService
        );
        ReflectionTestUtils.setField(authService, "appBaseUrl", "http://localhost:8080");
        authController = new AuthController(authService);
        userProfileService = new UserProfileService(userRepository, passwordEncoder);
    }

    private void wireRepositories() {
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(userSeq.getAndIncrement());
            }
            userStore.put(u.getId(), u);
            return u;
        });
        lenient().when(userRepository.findById(anyLong())).thenAnswer(inv ->
                Optional.ofNullable(userStore.get(inv.<Long>getArgument(0))));
        lenient().when(userRepository.findByEmailIgnoreCase(anyString())).thenAnswer(inv -> {
            String target = inv.<String>getArgument(0);
            return userStore.values().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(target))
                    .findFirst();
        });
        lenient().when(userRepository.findByEmailIgnoreCaseOrPhone(anyString(), anyString())).thenAnswer(inv -> {
            String email = inv.<String>getArgument(0);
            String phone = inv.<String>getArgument(1);
            return userStore.values().stream()
                    .filter(u -> (u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                            || (u.getPhone() != null && u.getPhone().equals(phone)))
                    .findFirst();
        });
        lenient().when(userRepository.existsByPhone(anyString())).thenAnswer(inv -> {
            String phone = inv.<String>getArgument(0);
            return userStore.values().stream().anyMatch(u -> phone.equals(u.getPhone()));
        });
        lenient().when(propertyRepository.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(tenantRepository.findFirstByEmailIgnoreCaseOrPhone(anyString(), anyString()))
                .thenReturn(Optional.empty());

        lenient().when(tokenRepository.save(any(AuthVerificationToken.class))).thenAnswer(inv -> {
            AuthVerificationToken t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(tokenSeq.getAndIncrement());
            }
            if (t.getCreatedAt() == null) {
                t.setCreatedAt(LocalDateTime.now());
            }
            t.prePersistDefaults();
            tokenStore.put(t.getId(), t);
            return t;
        });
        lenient().when(tokenRepository.saveAll(anyIterable())).thenAnswer(inv -> {
            Iterable<AuthVerificationToken> list = inv.getArgument(0);
            for (AuthVerificationToken t : list) {
                tokenStore.put(t.getId(), t);
            }
            return list;
        });
        lenient().when(tokenRepository.findById(anyLong())).thenAnswer(inv ->
                Optional.ofNullable(tokenStore.get(inv.<Long>getArgument(0))));
        lenient().when(tokenRepository.findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDescIdDesc(anyString(), any()))
                .thenAnswer(inv -> {
                    String email = inv.getArgument(0);
                    AuthVerificationToken.Purpose purpose = inv.getArgument(1);
                    return tokenStore.values().stream()
                            .filter(t -> t.getEmail().equalsIgnoreCase(email) && t.getPurpose() == purpose)
                            .max(Comparator.comparing(AuthVerificationToken::getCreatedAt)
                                    .thenComparing(AuthVerificationToken::getId));
                });
        lenient().when(tokenRepository
                .findTopByEmailIgnoreCaseAndPurposeAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
                        anyString(), any()))
                .thenAnswer(inv -> {
                    String email = inv.getArgument(0);
                    AuthVerificationToken.Purpose purpose = inv.getArgument(1);
                    return tokenStore.values().stream()
                            .filter(t -> t.getEmail().equalsIgnoreCase(email)
                                    && t.getPurpose() == purpose
                                    && !Boolean.TRUE.equals(t.getInvalidated())
                                    && t.getUsedAt() == null)
                            .max(Comparator.comparing(AuthVerificationToken::getCreatedAt)
                                    .thenComparing(AuthVerificationToken::getId));
                });
        lenient().when(tokenRepository
                .findTopByEmailIgnoreCaseAndPurposeAndResetTicketHashAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
                        anyString(), any(), anyString()))
                .thenAnswer(inv -> {
                    String email = inv.getArgument(0);
                    AuthVerificationToken.Purpose purpose = inv.getArgument(1);
                    String ticketHash = inv.getArgument(2);
                    return tokenStore.values().stream()
                            .filter(t -> t.getEmail().equalsIgnoreCase(email)
                                    && t.getPurpose() == purpose
                                    && ticketHash.equals(t.getResetTicketHash())
                                    && !Boolean.TRUE.equals(t.getInvalidated())
                                    && t.getUsedAt() == null)
                            .max(Comparator.comparing(AuthVerificationToken::getCreatedAt)
                                    .thenComparing(AuthVerificationToken::getId));
                });
        lenient().when(tokenRepository.countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(anyString(), any(), any()))
                .thenAnswer(inv -> {
                    String email = inv.getArgument(0);
                    AuthVerificationToken.Purpose purpose = inv.getArgument(1);
                    LocalDateTime after = inv.getArgument(2);
                    return tokenStore.values().stream()
                            .filter(t -> t.getEmail().equalsIgnoreCase(email)
                                    && t.getPurpose() == purpose
                                    && t.getCreatedAt() != null
                                    && t.getCreatedAt().isAfter(after))
                            .count();
                });
        lenient().when(tokenRepository.findByUserIdAndPurposeAndInvalidatedFalseAndUsedAtIsNull(anyLong(), any()))
                .thenAnswer(inv -> {
                    Long userId = inv.getArgument(0);
                    AuthVerificationToken.Purpose purpose = inv.getArgument(1);
                    return tokenStore.values().stream()
                            .filter(t -> t.getUser().getId().equals(userId)
                                    && t.getPurpose() == purpose
                                    && !Boolean.TRUE.equals(t.getInvalidated())
                                    && t.getUsedAt() == null)
                            .toList();
                });
    }

    @Test
    void registerWithGmail_requiresOtpVerification_hashesOtp_andAllowsLoginAfterVerify() {
        RegisterRequest nonGmail = new RegisterRequest("Nguyen A", "user@yahoo.com", "0901112233", "123456", "TENANT");
        assertThrows(RuntimeException.class, () -> authService.register(nonGmail));

        RegisterRequest req = new RegisterRequest("Nguyen Van An", "an.nguyen@gmail.com", "0901112233", "123456", "TENANT");
        AuthResponse regResp = authService.register(req);

        assertNull(regResp.getAccessToken());
        assertFalse(Boolean.TRUE.equals(regResp.getEmailVerified()));
        assertTrue(Boolean.TRUE.equals(regResp.getRequiresEmailVerification()));
        assertEquals(60, regResp.getResendCooldownSeconds());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendRegistrationVerificationEmail(
                eq("an.nguyen@gmail.com"), eq("Nguyen Van An"), otpCaptor.capture(), eq(10), urlCaptor.capture());

        String realOtp = otpCaptor.getValue();
        assertTrue(realOtp.matches("\\d{6}"));
        assertTrue(urlCaptor.getValue().contains("/api/auth/verify-email"));

        AuthVerificationToken stored = tokenStore.values().iterator().next();
        assertNotEquals(realOtp, stored.getCodeHash());
        assertEquals(64, stored.getCodeHash().length());

        // Chưa xác thực thì bị chặn đăng nhập
        assertThrows(UnverifiedAccountException.class,
                () -> authService.login(new LoginRequest("an.nguyen@gmail.com", "123456")));

        // Nhập sai OTP -> tăng số lần thử sai
        assertThrows(RuntimeException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest("an.nguyen@gmail.com", "000000")));
        assertEquals(1, stored.getAttemptCount());

        // Nhập đúng OTP -> kích hoạt tài khoản
        authService.verifyEmail(new VerifyEmailRequest("an.nguyen@gmail.com", realOtp));
        User verifiedUser = userRepository.findByEmailIgnoreCase("an.nguyen@gmail.com").orElseThrow();
        assertTrue(verifiedUser.getEmailVerified());
        assertNotNull(verifiedUser.getEmailVerifiedAt());
        assertNotNull(stored.getUsedAt());
        assertTrue(stored.getInvalidated());

        // Không cho phép dùng lại OTP đã sử dụng
        assertThrows(RuntimeException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest("an.nguyen@gmail.com", realOtp)));

        // Đăng nhập thành công sau khi xác thực
        AuthResponse loginResp = authService.login(new LoginRequest("an.nguyen@gmail.com", "123456"));
        assertNotNull(loginResp.getAccessToken());
        assertTrue(jwtUtil.validateToken(loginResp.getAccessToken()));
        assertEquals(0, jwtUtil.extractTokenVersion(loginResp.getAccessToken()));
    }

    @Test
    void resendVerification_andOtpLockoutAndExpiry_enforcedStrictly() {
        RegisterRequest req = new RegisterRequest("Tran Binh", "binh.tran@gmail.com", "0902223344", "123456", "LANDLORD");
        authService.register(req);

        ArgumentCaptor<String> firstOtpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendRegistrationVerificationEmail(
                eq("binh.tran@gmail.com"), anyString(), firstOtpCaptor.capture(), eq(10), anyString());
        String firstOtp = firstOtpCaptor.getValue();

        // Gửi lại ngay trong cooldown 60s -> 429 TooManyRequests
        assertThrows(TooManyRequestsException.class,
                () -> authService.resendVerification(new ResendVerificationRequest("binh.tran@gmail.com")));

        // Giả lập đã qua 65 giây
        AuthVerificationToken firstToken = tokenStore.values().iterator().next();
        firstToken.setCreatedAt(LocalDateTime.now().minusSeconds(65));
        authService.resendVerification(new ResendVerificationRequest("binh.tran@gmail.com"));

        assertTrue(firstToken.getInvalidated());
        assertThrows(RuntimeException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest("binh.tran@gmail.com", firstOtp)));

        // Kiểm tra OTP hết hạn
        AuthVerificationToken secondToken = tokenStore.values().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getInvalidated()))
                .findFirst()
                .orElseThrow();
        secondToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertThrows(RuntimeException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest("binh.tran@gmail.com", "123456")));

        // Đặt lại hạn và thử nhập sai quá 5 lần -> khóa OTP
        secondToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        for (int i = 0; i < 5; i++) {
            assertThrows(RuntimeException.class,
                    () -> authService.verifyEmail(new VerifyEmailRequest("binh.tran@gmail.com", "999999")));
        }
        assertTrue(secondToken.getInvalidated());
        assertEquals(5, secondToken.getAttemptCount());

        // Email đã xác thực -> không cho đăng ký trùng
        User verified = new User("Da Xac Thuc", "verified.user@gmail.com", "0903334455",
                passwordEncoder.encode("123456"), Role.TENANT);
        verified.setId(99L);
        verified.setEmailVerified(true);
        userStore.put(verified.getId(), verified);

        RegisterRequest duplicateReq = new RegisterRequest("Dup User", "verified.user@gmail.com", "0904445566", "123456", "TENANT");
        assertThrows(RuntimeException.class, () -> authService.register(duplicateReq));
    }

    @Test
    void forgotPasswordAndResetPassword_rotatesTokenVersionAndInvalidatesOldCredentials() {
        User target = new User("Le Minh", "minh.le@gmail.com", "0905556677",
                passwordEncoder.encode("oldPass123"), Role.TENANT);
        target.setId(50L);
        target.setEmailVerified(true);
        target.setTokenVersion(0);
        userStore.put(target.getId(), target);

        User other = new User("Other User", "other.user@gmail.com", "0906667788",
                passwordEncoder.encode("otherPass"), Role.TENANT);
        other.setId(51L);
        other.setEmailVerified(true);
        userStore.put(other.getId(), other);

        AuthResponse oldLogin = authService.login(new LoginRequest("minh.le@gmail.com", "oldPass123"));
        String oldJwt = oldLogin.getAccessToken();
        assertEquals(0, jwtUtil.extractTokenVersion(oldJwt));

        // Email chưa đăng ký -> không báo lỗi và không gọi emailService
        authService.forgotPassword(new ForgotPasswordRequest("nobody.exists@gmail.com"));
        verify(emailService, never()).sendPasswordResetEmail(eq("nobody.exists@gmail.com"), any(), any(), anyInt());

        // Yêu cầu khôi phục mật khẩu hợp lệ
        authService.forgotPassword(new ForgotPasswordRequest("minh.le@gmail.com"));
        ArgumentCaptor<String> resetOtpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendPasswordResetEmail(
                eq("minh.le@gmail.com"), eq("Le Minh"), resetOtpCaptor.capture(), eq(10));
        String resetOtp = resetOtpCaptor.getValue();

        // Xác minh OTP -> cấp resetToken 1 lần
        VerifyResetOtpResponse verifyResp = authService.verifyResetOtp(
                new VerifyResetOtpRequest("minh.le@gmail.com", resetOtp));
        assertNotNull(verifyResp.resetToken());
        assertEquals(64, verifyResp.resetToken().length());

        // Không cho phép dùng resetToken của user A cho email user B
        assertThrows(RuntimeException.class, () -> authService.resetPassword(
                new ResetPasswordRequest("other.user@gmail.com", verifyResp.resetToken(), "newPass999", "newPass999")));

        // Đặt lại mật khẩu thành công
        authService.resetPassword(
                new ResetPasswordRequest("minh.le@gmail.com", verifyResp.resetToken(), "newPass999", "newPass999"));

        // Không dùng lại được OTP hoặc resetToken đã tiêu thụ
        assertThrows(RuntimeException.class, () -> authService.verifyResetOtp(
                new VerifyResetOtpRequest("minh.le@gmail.com", resetOtp)));
        assertThrows(RuntimeException.class, () -> authService.resetPassword(
                new ResetPasswordRequest("minh.le@gmail.com", verifyResp.resetToken(), "another1", "another1")));

        // Mật khẩu cũ không còn hợp lệ; phiên JWT cũ bị vô hiệu hóa vì tokenVersion tăng 0 -> 1
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("minh.le@gmail.com", "oldPass123")));

        User updated = userRepository.findById(50L).orElseThrow();
        assertEquals(1, updated.getTokenVersion());
        assertNotEquals(jwtUtil.extractTokenVersion(oldJwt), updated.getTokenVersion());

        AuthResponse newLogin = authService.login(new LoginRequest("minh.le@gmail.com", "newPass999"));
        assertEquals(1, jwtUtil.extractTokenVersion(newLogin.getAccessToken()));
    }

    @Test
    void legacyAccountRegression_andProfileChangePasswordRevokesTokenVersion() {
        User legacy = new User("Nguyen Van A", "landlord1@example.com", "0901234567",
                passwordEncoder.encode("123456"), Role.LANDLORD);
        legacy.setId(1L);
        legacy.setEmailVerified(true);
        legacy.setTokenVersion(0);
        userStore.put(legacy.getId(), legacy);

        AuthResponse loginResp = authService.login(new LoginRequest("landlord1@example.com", "123456"));
        assertNotNull(loginResp.getAccessToken());
        assertTrue(Boolean.TRUE.equals(loginResp.getEmailVerified()));

        userProfileService.changePassword(legacy, new ChangePasswordRequest("123456", "654321"));
        assertEquals(1, legacy.getTokenVersion());
        assertTrue(passwordEncoder.matches("654321", legacy.getPassword()));
    }

    @Test
    void gmailServiceMissingSmtpConfig_andControllerHttpStatuses_handledSafely() throws Exception {
        GmailEmailService unconfiguredService = new GmailEmailService(
                mailSenderProvider,
                "",
                "",
                "",
                "RoomCare"
        );
        assertFalse(unconfiguredService.isConfigured());
        assertThrows(MailDeliveryException.class, () ->
                unconfiguredService.sendRegistrationVerificationEmail("a@gmail.com", "User", "123456", 10, "http://localhost"));

        User unverified = new User("Unverified User", "pending@gmail.com", "0908889900",
                passwordEncoder.encode("123456"), Role.TENANT);
        unverified.setId(77L);
        unverified.setEmailVerified(false);
        userStore.put(unverified.getId(), unverified);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // Login unverified -> 403 + requiresEmailVerification=true
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("pending@gmail.com", "123456"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.requiresEmailVerification").value(true))
                .andExpect(jsonPath("$.data.email").value("pending@gmail.com"));

        // Forgot password với email chưa tồn tại -> vẫn trả 200 với thông điệp trung lập
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("unknown@gmail.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
