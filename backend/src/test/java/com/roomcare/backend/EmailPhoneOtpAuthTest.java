package com.roomcare.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomcare.backend.auth.AuthController;
import com.roomcare.backend.auth.AuthService;
import com.roomcare.backend.dto.ForgotPasswordRequest;
import com.roomcare.backend.dto.LoginRequest;
import com.roomcare.backend.dto.RegisterRequest;
import com.roomcare.backend.dto.ResendOtpRequest;
import com.roomcare.backend.dto.ResetPasswordRequest;
import com.roomcare.backend.dto.VerifyOtpRequest;
import com.roomcare.backend.dto.VerifyResetOtpRequest;
import com.roomcare.backend.dto.user.ProfileResponse;
import com.roomcare.backend.dto.user.RequestContactOtpRequest;
import com.roomcare.backend.dto.user.UpdateProfileRequest;
import com.roomcare.backend.dto.user.VerifyContactOtpRequest;
import com.roomcare.backend.entity.AuthVerificationToken;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.Role;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.exception.GlobalExceptionHandler;
import com.roomcare.backend.exception.InvalidCredentialsException;
import com.roomcare.backend.exception.MailDeliveryException;
import com.roomcare.backend.exception.SmsDeliveryException;
import com.roomcare.backend.exception.TooManyRequestsException;
import com.roomcare.backend.exception.UnverifiedAccountException;
import com.roomcare.backend.repository.AuthVerificationTokenRepository;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.TenantRepository;
import com.roomcare.backend.repository.UserRepository;
import com.roomcare.backend.security.JwtUtil;
import com.roomcare.backend.service.EmailService;
import com.roomcare.backend.service.GmailEmailService;
import com.roomcare.backend.service.OtpAttemptService;
import com.roomcare.backend.service.OtpVerificationService;
import com.roomcare.backend.service.SmsOtpProvider;
import com.roomcare.backend.service.TwilioSmsOtpProvider;
import com.roomcare.backend.service.UserProfileService;
import com.roomcare.backend.util.PhoneNumberUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
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

/**
 * Kiểm thử toàn diện luồng xác thực Email (Gmail) và Số điện thoại (+84) bằng OTP,
 * bao gồm bảo mật hash OTP, khóa sai 5 lần, cô lập kênh/mục đích/người dùng,
 * rate limit theo tài khoản/IP/kênh, quên mật khẩu và vô hiệu hóa JWT cũ.
 */
class EmailPhoneOtpAuthTest {

    private UserRepository userRepository;
    private AuthVerificationTokenRepository tokenRepository;
    private PropertyRepository propertyRepository;
    private TenantRepository tenantRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private EmailService emailService;
    private SmsOtpProvider smsOtpProvider;
    private OtpAttemptService otpAttemptService;
    private OtpVerificationService otpVerificationService;
    private AuthService authService;
    private UserProfileService userProfileService;
    private MockMvc mockMvc;

    private final Map<Long, User> userStore = new LinkedHashMap<>();
    private final Map<Long, AuthVerificationToken> tokenStore = new LinkedHashMap<>();
    private final List<SentEmailRecord> sentEmails = new ArrayList<>();
    private final List<SentSmsRecord> sentSms = new ArrayList<>();
    private final AtomicLong userIdSeq = new AtomicLong(10L);
    private final AtomicLong tokenIdSeq = new AtomicLong(100L);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private record SentEmailRecord(String type, String to, String otp, String verificationUrl) {}
    private record SentSmsRecord(String phone, String otpOrNull, String purpose) {}

    @BeforeEach
    void setUp() {
        userStore.clear();
        tokenStore.clear();
        sentEmails.clear();
        sentSms.clear();
        userIdSeq.set(10L);
        tokenIdSeq.set(100L);

        userRepository = mock(UserRepository.class, withSettings().lenient());
        tokenRepository = mock(AuthVerificationTokenRepository.class, withSettings().lenient());
        propertyRepository = mock(PropertyRepository.class, withSettings().lenient());
        tenantRepository = mock(TenantRepository.class, withSettings().lenient());
        passwordEncoder = new BCryptPasswordEncoder();

        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "roomcare-super-secret-jwt-key-for-testing-2026-hmac256");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        configureInMemoryRepositories();

        emailService = createFakeEmailService();
        smsOtpProvider = createLocalFakeSmsProvider();
        initServices(5, 60, 5, 20, 5);
    }

    private void initServices(
            int expMinutes,
            int cooldownSeconds,
            int maxPerHour,
            int maxPerIpPerHour,
            int maxAttempts
    ) {
        otpAttemptService = new OtpAttemptService(tokenRepository);
        otpVerificationService = new OtpVerificationService(
                tokenRepository,
                otpAttemptService,
                smsOtpProvider,
                expMinutes,
                cooldownSeconds,
                maxPerHour,
                maxPerIpPerHour,
                maxAttempts,
                "unit-test-pepper"
        );
        authService = new AuthService(
                userRepository,
                propertyRepository,
                tenantRepository,
                passwordEncoder,
                jwtUtil,
                otpVerificationService,
                emailService,
                smsOtpProvider
        );
        ReflectionTestUtils.setField(authService, "appBaseUrl", "http://localhost:8080");

        userProfileService = new UserProfileService(
                userRepository,
                passwordEncoder,
                otpVerificationService,
                emailService,
                smsOtpProvider
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void configureInMemoryRepositories() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(userIdSeq.getAndIncrement());
            }
            if (u.getCreatedAt() == null) {
                u.setCreatedAt(LocalDateTime.now());
            }
            u.setUpdatedAt(LocalDateTime.now());
            userStore.put(u.getId(), u);
            return u;
        });
        when(userRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(userStore.get(inv.<Long>getArgument(0))));
        when(userRepository.findByEmailIgnoreCase(anyString())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            if (email == null) return Optional.empty();
            return userStore.values().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        });
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            return email != null && userStore.values().stream()
                    .anyMatch(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email));
        });
        when(userRepository.findFirstByPhoneIn(anyCollection())).thenAnswer(inv -> {
            Collection<String> variants = inv.getArgument(0);
            if (variants == null || variants.isEmpty()) return Optional.empty();
            return userStore.values().stream()
                    .filter(u -> u.getPhone() != null && variants.contains(u.getPhone()))
                    .findFirst();
        });
        when(userRepository.existsByPhoneIn(anyCollection())).thenAnswer(inv -> {
            Collection<String> variants = inv.getArgument(0);
            return variants != null && userStore.values().stream()
                    .anyMatch(u -> u.getPhone() != null && variants.contains(u.getPhone()));
        });

        Comparator<AuthVerificationToken> latestFirst = Comparator
                .comparing(AuthVerificationToken::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AuthVerificationToken::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();

        when(tokenRepository.save(any(AuthVerificationToken.class))).thenAnswer(inv -> {
            AuthVerificationToken t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(tokenIdSeq.getAndIncrement());
            }
            if (t.getCreatedAt() == null) {
                t.setCreatedAt(LocalDateTime.now());
            }
            tokenStore.put(t.getId(), t);
            return t;
        });
        when(tokenRepository.saveAll(anyIterable())).thenAnswer(inv -> {
            Iterable<AuthVerificationToken> list = inv.getArgument(0);
            List<AuthVerificationToken> result = new ArrayList<>();
            for (AuthVerificationToken t : list) {
                if (t.getId() == null) {
                    t.setId(tokenIdSeq.getAndIncrement());
                }
                if (t.getCreatedAt() == null) {
                    t.setCreatedAt(LocalDateTime.now());
                }
                tokenStore.put(t.getId(), t);
                result.add(t);
            }
            return result;
        });
        when(tokenRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(tokenStore.get(inv.<Long>getArgument(0))));
        when(tokenRepository.findTopByTargetIgnoreCaseAndChannelOrderByCreatedAtDescIdDesc(anyString(), any()))
                .thenAnswer(inv -> {
                    String target = inv.getArgument(0);
                    AuthVerificationToken.Channel ch = inv.getArgument(1);
                    return tokenStore.values().stream()
                            .filter(t -> t.getChannel() == ch && t.getTarget() != null && t.getTarget().equalsIgnoreCase(target))
                            .sorted(latestFirst)
                            .findFirst();
                });
        when(tokenRepository.findTopByTargetIgnoreCaseAndChannelAndPurposeAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
                anyString(), any(), any()))
                .thenAnswer(inv -> {
                    String target = inv.getArgument(0);
                    AuthVerificationToken.Channel ch = inv.getArgument(1);
                    AuthVerificationToken.Purpose purpose = inv.getArgument(2);
                    return tokenStore.values().stream()
                            .filter(t -> t.getChannel() == ch
                                    && t.getPurpose() == purpose
                                    && t.getTarget() != null && t.getTarget().equalsIgnoreCase(target)
                                    && !Boolean.TRUE.equals(t.getInvalidated())
                                    && t.getUsedAt() == null)
                            .sorted(latestFirst)
                            .findFirst();
                });
        when(tokenRepository.findTopByTargetIgnoreCaseAndChannelAndPurposeAndResetTicketHashAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
                anyString(), any(), any(), anyString()))
                .thenAnswer(inv -> {
                    String target = inv.getArgument(0);
                    AuthVerificationToken.Channel ch = inv.getArgument(1);
                    AuthVerificationToken.Purpose purpose = inv.getArgument(2);
                    String ticketHash = inv.getArgument(3);
                    return tokenStore.values().stream()
                            .filter(t -> t.getChannel() == ch
                                    && t.getPurpose() == purpose
                                    && t.getTarget() != null && t.getTarget().equalsIgnoreCase(target)
                                    && Objects.equals(t.getResetTicketHash(), ticketHash)
                                    && !Boolean.TRUE.equals(t.getInvalidated())
                                    && t.getUsedAt() == null)
                            .sorted(latestFirst)
                            .findFirst();
                });
        when(tokenRepository.countByTargetIgnoreCaseAndChannelAndCreatedAtAfter(anyString(), any(), any()))
                .thenAnswer(inv -> {
                    String target = inv.getArgument(0);
                    AuthVerificationToken.Channel ch = inv.getArgument(1);
                    LocalDateTime after = inv.getArgument(2);
                    return tokenStore.values().stream()
                            .filter(t -> t.getChannel() == ch
                                    && t.getTarget() != null && t.getTarget().equalsIgnoreCase(target)
                                    && t.getCreatedAt() != null && t.getCreatedAt().isAfter(after))
                            .count();
                });
        when(tokenRepository.countByClientIpAndChannelAndCreatedAtAfter(anyString(), any(), any()))
                .thenAnswer(inv -> {
                    String ip = inv.getArgument(0);
                    AuthVerificationToken.Channel ch = inv.getArgument(1);
                    LocalDateTime after = inv.getArgument(2);
                    return tokenStore.values().stream()
                            .filter(t -> t.getChannel() == ch
                                    && Objects.equals(t.getClientIp(), ip)
                                    && t.getCreatedAt() != null && t.getCreatedAt().isAfter(after))
                            .count();
                });
        when(tokenRepository.findByUserIdAndChannelAndPurposeAndInvalidatedFalseAndUsedAtIsNull(anyLong(), any(), any()))
                .thenAnswer(inv -> {
                    Long uid = inv.getArgument(0);
                    AuthVerificationToken.Channel ch = inv.getArgument(1);
                    AuthVerificationToken.Purpose purpose = inv.getArgument(2);
                    return tokenStore.values().stream()
                            .filter(t -> t.getUser() != null && Objects.equals(t.getUser().getId(), uid)
                                    && t.getChannel() == ch
                                    && t.getPurpose() == purpose
                                    && !Boolean.TRUE.equals(t.getInvalidated())
                                    && t.getUsedAt() == null)
                            .toList();
                });

        when(propertyRepository.save(any(Property.class))).thenAnswer(inv -> {
            Property p = inv.getArgument(0);
            if (p.getId() == null) p.setId(500L);
            return p;
        });
        when(tenantRepository.findFirstByEmailIgnoreCaseOrPhone(any(), any())).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private EmailService createFakeEmailService() {
        return new EmailService() {
            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public void sendRegistrationVerificationEmail(
                    String toEmail, String fullName, String otpCode, int expirationMinutes, String verificationUrl) {
                sentEmails.add(new SentEmailRecord("REGISTER", toEmail, otpCode, verificationUrl));
            }

            @Override
            public void sendPasswordResetEmail(String toEmail, String fullName, String otpCode, int expirationMinutes) {
                sentEmails.add(new SentEmailRecord("RESET", toEmail, otpCode, null));
            }

            @Override
            public void sendContactVerificationEmail(String toEmail, String fullName, String otpCode, int expirationMinutes) {
                sentEmails.add(new SentEmailRecord("CONTACT", toEmail, otpCode, null));
            }
        };
    }

    private SmsOtpProvider createLocalFakeSmsProvider() {
        return new SmsOtpProvider() {
            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public boolean isProviderManagedVerification() {
                return false;
            }

            @Override
            public String providerName() {
                return "TWILIO_SMS";
            }

            @Override
            public String sendOtp(String e164Phone, String otpCodeOrNull, String purposeLabel, int expirationMinutes) {
                sentSms.add(new SentSmsRecord(e164Phone, otpCodeOrNull, purposeLabel));
                return "SM_TEST_" + sentSms.size();
            }

            @Override
            public boolean verifyProviderOtp(String e164Phone, String otpCode) {
                return false;
            }
        };
    }

    @Test
    void registerAndVerifyViaGmail_createsEmailOnlyUserHashesOtpAndBlocksLoginUntilVerified() {
        RegisterRequest req = new RegisterRequest("Nguyễn Minh Anh", "MinhAnh.Dev@gmail.com", null, "Secret123", "TENANT");
        req.setVerificationMethod("EMAIL");

        var pending = authService.register(req, "192.168.1.10");

        assertTrue(Boolean.TRUE.equals(pending.getRequiresVerification()));
        assertNull(pending.getAccessToken());
        assertEquals("EMAIL", pending.getVerificationChannel());
        assertEquals("minhanh.dev@gmail.com", pending.getVerificationTarget());
        assertEquals(60, pending.getResendCooldownSeconds());

        User saved = userStore.values().iterator().next();
        assertEquals("minhanh.dev@gmail.com", saved.getEmail());
        assertNull(saved.getPhone());
        assertFalse(Boolean.TRUE.equals(saved.getEmailVerified()));
        assertFalse(saved.isAccountVerified());

        assertEquals(1, sentEmails.size());
        String otp = sentEmails.get(0).otp();
        assertTrue(otp.matches("^\\d{6}$"));
        assertTrue(sentEmails.get(0).verificationUrl().contains("otp=" + otp));

        AuthVerificationToken storedToken = tokenStore.values().iterator().next();
        assertNotEquals(otp, storedToken.getCodeHash());
        assertEquals(64, storedToken.getCodeHash().length());
        assertEquals(5, storedToken.getMaxAttempts());
        assertEquals("192.168.1.10", storedToken.getClientIp());

        assertThrows(UnverifiedAccountException.class,
                () -> authService.login(new LoginRequest("minhanh.dev@gmail.com", "Secret123")));

        authService.verifyOtp(new VerifyOtpRequest(AuthVerificationToken.Channel.EMAIL, "minhanh.dev@gmail.com", otp));

        assertTrue(Boolean.TRUE.equals(saved.getEmailVerified()));
        assertTrue(saved.isAccountVerified());

        var loginRes = authService.login(new LoginRequest("minhanh.dev@gmail.com", "Secret123"));
        assertNotNull(loginRes.getAccessToken());
        assertFalse(Boolean.TRUE.equals(loginRes.getRequiresVerification()));
        assertEquals("minhanh.dev@gmail.com", jwtUtil.extractEmail(loginRes.getAccessToken()));
        assertEquals(saved.getId(), jwtUtil.extractUserId(loginRes.getAccessToken()));
    }

    @Test
    void registerAndVerifyViaVietnamPhone_normalizesLocalToE164AndLogsInWithBothFormats() {
        RegisterRequest req = new RegisterRequest("Trần Quốc Bảo", null, "0987 654 321", "Secret123", "LANDLORD");
        req.setVerificationMethod("PHONE");

        var pending = authService.register(req, "10.0.0.5");

        assertTrue(Boolean.TRUE.equals(pending.getRequiresVerification()));
        assertEquals("PHONE", pending.getVerificationChannel());
        assertEquals("+84987654321", pending.getVerificationTarget());

        User saved = userStore.values().iterator().next();
        assertNull(saved.getEmail());
        assertEquals("+84987654321", saved.getPhone());
        assertFalse(Boolean.TRUE.equals(saved.getPhoneVerified()));
        verify(propertyRepository, times(1)).save(any(Property.class));

        assertEquals(1, sentSms.size());
        assertEquals("+84987654321", sentSms.get(0).phone());
        String otp = sentSms.get(0).otpOrNull();
        assertTrue(otp.matches("^\\d{6}$"));

        assertThrows(UnverifiedAccountException.class,
                () -> authService.login(new LoginRequest("0987654321", "Secret123")));

        authService.verifyOtp(new VerifyOtpRequest(AuthVerificationToken.Channel.PHONE, "+84987654321", otp));
        assertTrue(saved.isAccountVerified());

        var loginByLocal = authService.login(new LoginRequest("0987654321", "Secret123"));
        var loginByE164 = authService.login(new LoginRequest("+84987654321", "Secret123"));
        assertNotNull(loginByLocal.getAccessToken());
        assertNotNull(loginByE164.getAccessToken());
        assertEquals("+84987654321", loginByLocal.getPhone());
        assertEquals(List.of("+84987654321", "0987654321"), PhoneNumberUtils.lookupVariants("0987654321"));
    }

    @Test
    void registerAndVerifyViaTwilioVerifyProviderManaged_storesSaltedNonPlaintextHashAndVerifiesViaProvider() {
        smsOtpProvider = new SmsOtpProvider() {
            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public boolean isProviderManagedVerification() {
                return true;
            }

            @Override
            public String providerName() {
                return "TWILIO_VERIFY";
            }

            @Override
            public String sendOtp(String e164Phone, String otpCodeOrNull, String purposeLabel, int expirationMinutes) {
                sentSms.add(new SentSmsRecord(e164Phone, otpCodeOrNull, purposeLabel));
                return "VE_TEST_999";
            }

            @Override
            public boolean verifyProviderOtp(String e164Phone, String otpCode) {
                return "+84912345678".equals(e164Phone) && "654321".equals(otpCode);
            }
        };
        initServices(5, 60, 5, 20, 5);

        RegisterRequest req = new RegisterRequest("Lê Hoàng", null, "0912345678", "Pass123", "TENANT");
        req.setVerificationMethod("PHONE");
        authService.register(req, "10.0.0.6");

        assertEquals(1, sentSms.size());
        assertNull(sentSms.get(0).otpOrNull());
        AuthVerificationToken token = tokenStore.values().iterator().next();
        assertEquals("TWILIO_VERIFY", token.getProvider());
        assertEquals("VE_TEST_999", token.getProviderReference());
        assertEquals(64, token.getCodeHash().length());

        assertThrows(RuntimeException.class,
                () -> authService.verifyOtp(new VerifyOtpRequest(AuthVerificationToken.Channel.PHONE, "0912345678", "111111")));
        assertEquals(1, tokenStore.get(token.getId()).getAttemptCount());

        authService.verifyOtp(new VerifyOtpRequest(AuthVerificationToken.Channel.PHONE, "+84912345678", "654321"));
        User saved = userStore.values().iterator().next();
        assertTrue(Boolean.TRUE.equals(saved.getPhoneVerified()));
    }

    @Test
    void otpSecurity_locksAfterFiveWrongAttempts_rejectsExpired_andSingleUseOnly() {
        RegisterRequest req = new RegisterRequest("Phạm Lan", "phamlan.test@gmail.com", null, "Secret123", "TENANT");
        req.setVerificationMethod("EMAIL");
        authService.register(req, "127.0.0.1");

        String correctOtp = sentEmails.get(0).otp();
        String wrongOtp = "000000".equals(correctOtp) ? "111111" : "000000";
        AuthVerificationToken token = tokenStore.values().iterator().next();

        for (int i = 1; i <= 5; i++) {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.verifyOtp(new VerifyOtpRequest(AuthVerificationToken.Channel.EMAIL, "phamlan.test@gmail.com", wrongOtp)));
            if (i < 5) {
                assertTrue(ex.getMessage().contains("còn " + (5 - i)));
            } else {
                assertTrue(ex.getMessage().contains("quá 5 lần"));
            }
        }
        assertTrue(Boolean.TRUE.equals(tokenStore.get(token.getId()).getInvalidated()));

        // Even with the right OTP after 5 wrong tries, must fail because locked
        assertThrows(RuntimeException.class,
                () -> authService.verifyOtp(new VerifyOtpRequest(AuthVerificationToken.Channel.EMAIL, "phamlan.test@gmail.com", correctOtp)));

        // Issue a fresh OTP after clearing cooldown timestamp and test expiry + single use
        token.setCreatedAt(LocalDateTime.now().minusSeconds(90));
        authService.resendOtp(new ResendOtpRequest(AuthVerificationToken.Channel.EMAIL, "phamlan.test@gmail.com"), "127.0.0.1");
        String freshOtp = sentEmails.get(1).otp();
        AuthVerificationToken freshToken = tokenStore.values().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getInvalidated()))
                .findFirst().orElseThrow();

        freshToken.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        RuntimeException expiredEx = assertThrows(RuntimeException.class,
                () -> authService.verifyOtp(new VerifyOtpRequest(AuthVerificationToken.Channel.EMAIL, "phamlan.test@gmail.com", freshOtp)));
        assertTrue(expiredEx.getMessage().contains("hết hạn"));

        // Make valid again, verify once (succeeds), then replay fails (single-use)
        freshToken.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        authService.verifyOtp(new VerifyOtpRequest(AuthVerificationToken.Channel.EMAIL, "phamlan.test@gmail.com", freshOtp));
        assertNotNull(freshToken.getUsedAt());
        assertThrows(RuntimeException.class,
                () -> authService.verifyOtp(new VerifyOtpRequest(AuthVerificationToken.Channel.EMAIL, "phamlan.test@gmail.com", freshOtp)));
    }

    @Test
    void otpIsolation_crossChannelCrossPurposeAndCrossUserRejected() {
        User rawU1 = new User();
        rawU1.setFullName("User One");
        rawU1.setEmail("userone@gmail.com");
        rawU1.setPhone("+84901112233");
        rawU1.setPassword(passwordEncoder.encode("Secret123"));
        rawU1.setRole(Role.TENANT);
        rawU1.setEmailVerified(false);
        rawU1.setPhoneVerified(false);
        User u1 = userRepository.save(rawU1);

        User rawU2 = new User();
        rawU2.setFullName("User Two");
        rawU2.setEmail("usertwo@gmail.com");
        rawU2.setPassword(passwordEncoder.encode("Secret123"));
        rawU2.setRole(Role.TENANT);
        rawU2.setEmailVerified(false);
        rawU2.setPhoneVerified(false);
        User u2 = userRepository.save(rawU2);

        var issuedEmailReg = otpVerificationService.createTokenAndDispatchPreparation(
                u1, AuthVerificationToken.Channel.EMAIL, AuthVerificationToken.Purpose.REGISTRATION, u1.getEmail(), "1.1.1.1");

        // Cross-channel rejection (EMAIL registration OTP used as PHONE)
        assertThrows(RuntimeException.class, () -> otpVerificationService.verifyOtp(
                AuthVerificationToken.Channel.PHONE, AuthVerificationToken.Purpose.REGISTRATION, u1.getPhone(), issuedEmailReg.rawOtpOrNull()));

        // Cross-purpose rejection (REGISTRATION OTP used for PASSWORD_RESET)
        assertThrows(RuntimeException.class, () -> otpVerificationService.verifyOtp(
                AuthVerificationToken.Channel.EMAIL, AuthVerificationToken.Purpose.PASSWORD_RESET, u1.getEmail(), issuedEmailReg.rawOtpOrNull()));

        // Cross-user rejection (user1 OTP used for user2 email)
        String u2Email = u2.getEmail();
        assertThrows(RuntimeException.class, () -> otpVerificationService.verifyOtp(
                AuthVerificationToken.Channel.EMAIL, AuthVerificationToken.Purpose.REGISTRATION, u2Email, issuedEmailReg.rawOtpOrNull()));
    }

    @Test
    void rateLimiting_cooldownPerAccountAndPerIpEnforcedAndIsolatedByChannel() {
        initServices(5, 60, 3, 4, 5);

        User u1 = new User();
        u1.setFullName("Rate Limit User");
        u1.setEmail("ratelimit1@gmail.com");
        u1.setPhone("+84988887777");
        u1.setPassword(passwordEncoder.encode("Secret123"));
        u1.setRole(Role.LANDLORD);
        u1.setEmailVerified(false);
        u1.setPhoneVerified(false);
        u1 = userRepository.save(u1);

        // First EMAIL OTP succeeds
        otpVerificationService.createTokenAndDispatchPreparation(
                u1, AuthVerificationToken.Channel.EMAIL, AuthVerificationToken.Purpose.REGISTRATION, u1.getEmail(), "11.22.33.44");

        // Immediate resend -> 429 cooldown
        User finalU1 = u1;
        TooManyRequestsException cooldownEx = assertThrows(TooManyRequestsException.class,
                () -> otpVerificationService.createTokenAndDispatchPreparation(
                        finalU1, AuthVerificationToken.Channel.EMAIL, AuthVerificationToken.Purpose.REGISTRATION, finalU1.getEmail(), "11.22.33.44"));
        assertTrue(cooldownEx.getRetryAfterSeconds() > 0);

        // PHONE channel on the same user and IP is isolated from EMAIL limits
        otpVerificationService.createTokenAndDispatchPreparation(
                u1, AuthVerificationToken.Channel.PHONE, AuthVerificationToken.Purpose.REGISTRATION, u1.getPhone(), "11.22.33.44");

        // Age existing EMAIL tokens past cooldown (but inside 1 hour window) until per-account hourly limit (3) hits
        for (int i = 0; i < 2; i++) {
            tokenStore.values().stream()
                    .filter(t -> t.getChannel() == AuthVerificationToken.Channel.EMAIL)
                    .forEach(t -> t.setCreatedAt(LocalDateTime.now().minusSeconds(65)));
            otpVerificationService.createTokenAndDispatchPreparation(
                    u1, AuthVerificationToken.Channel.EMAIL, AuthVerificationToken.Purpose.REGISTRATION, u1.getEmail(), "11.22.33.44");
        }
        tokenStore.values().stream()
                .filter(t -> t.getChannel() == AuthVerificationToken.Channel.EMAIL)
                .forEach(t -> t.setCreatedAt(LocalDateTime.now().minusSeconds(65)));
        TooManyRequestsException hourlyTargetEx = assertThrows(TooManyRequestsException.class,
                () -> otpVerificationService.createTokenAndDispatchPreparation(
                        finalU1, AuthVerificationToken.Channel.EMAIL, AuthVerificationToken.Purpose.REGISTRATION, finalU1.getEmail(), "11.22.33.44"));
        assertTrue(hourlyTargetEx.getMessage().contains("vượt quá giới hạn"));

        // Different account on same IP "11.22.33.44": already has 3 EMAIL sends -> 4th succeeds, 5th hits IP limit (4)
        User u2 = new User();
        u2.setFullName("Second IP User");
        u2.setEmail("ratelimit2@gmail.com");
        u2.setPassword(passwordEncoder.encode("Secret123"));
        u2.setRole(Role.TENANT);
        u2.setEmailVerified(false);
        u2 = userRepository.save(u2);

        otpVerificationService.createTokenAndDispatchPreparation(
                u2, AuthVerificationToken.Channel.EMAIL, AuthVerificationToken.Purpose.REGISTRATION, u2.getEmail(), "11.22.33.44");
        User u3 = new User();
        u3.setFullName("Third IP User");
        u3.setEmail("ratelimit3@gmail.com");
        u3.setPassword(passwordEncoder.encode("Secret123"));
        u3.setRole(Role.TENANT);
        u3.setEmailVerified(false);
        u3 = userRepository.save(u3);
        User finalU3 = u3;
        TooManyRequestsException ipLimitEx = assertThrows(TooManyRequestsException.class,
                () -> otpVerificationService.createTokenAndDispatchPreparation(
                        finalU3, AuthVerificationToken.Channel.EMAIL, AuthVerificationToken.Purpose.REGISTRATION, finalU3.getEmail(), "11.22.33.44"));
        assertTrue(ipLimitEx.getMessage().contains("Thiết bị/IP"));
    }

    @Test
    void forgotPasswordAndReset_gmailAndPhone_antiEnumerationSingleUseAndRevokesOldJwt() {
        // Unknown accounts must return without throwing (anti-enumeration)
        assertDoesNotThrow(() -> authService.forgotPassword(
                new ForgotPasswordRequest(AuthVerificationToken.Channel.EMAIL, "nonexistent@gmail.com"), "127.0.0.1"));
        assertDoesNotThrow(() -> authService.forgotPassword(
                new ForgotPasswordRequest(AuthVerificationToken.Channel.PHONE, "0999888777"), "127.0.0.1"));
        assertTrue(sentEmails.isEmpty());
        assertTrue(sentSms.isEmpty());

        // Verified Gmail user + old JWT
        User user = new User();
        user.setFullName("Hoàng Nam");
        user.setEmail("hoangnam@gmail.com");
        user.setPassword(passwordEncoder.encode("OldPass123"));
        user.setRole(Role.LANDLORD);
        user.setEmailVerified(true);
        user.setPhoneVerified(false);
        user.setTokenVersion(0);
        user = userRepository.save(user);

        var oldLogin = authService.login(new LoginRequest("hoangnam@gmail.com", "OldPass123"));
        int oldTokenVersion = jwtUtil.extractTokenVersion(oldLogin.getAccessToken());
        assertEquals(0, oldTokenVersion);

        // Step 1: request password reset OTP
        authService.forgotPassword(new ForgotPasswordRequest(AuthVerificationToken.Channel.EMAIL, "hoangnam@gmail.com"), "127.0.0.1");
        assertEquals(1, sentEmails.size());
        String otp = sentEmails.get(0).otp();

        var verifyRes = authService.verifyResetOtp(
                new VerifyResetOtpRequest(AuthVerificationToken.Channel.EMAIL, "hoangnam@gmail.com", otp));
        assertNotNull(verifyRes.resetToken());
        assertFalse(verifyRes.resetToken().isBlank());

        // Mismatch confirm password must fail without consuming reset ticket
        assertThrows(RuntimeException.class, () -> authService.resetPassword(
                new ResetPasswordRequest(AuthVerificationToken.Channel.EMAIL, "hoangnam@gmail.com",
                        verifyRes.resetToken(), "NewPass123", "Different123")));

        // Valid password reset increments tokenVersion -> revokes old JWT
        authService.resetPassword(new ResetPasswordRequest(
                AuthVerificationToken.Channel.EMAIL, "hoangnam@gmail.com", verifyRes.resetToken(), "NewPass123", "NewPass123"));

        assertEquals(1, user.getTokenVersion());
        assertNotEquals(user.getTokenVersion(), jwtUtil.extractTokenVersion(oldLogin.getAccessToken()));

        // Reset ticket is single-use
        assertThrows(RuntimeException.class, () -> authService.resetPassword(new ResetPasswordRequest(
                AuthVerificationToken.Channel.EMAIL, "hoangnam@gmail.com", verifyRes.resetToken(), "NewPass456", "NewPass456")));

        // Old password fails, new password issues JWT with tv=1
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("hoangnam@gmail.com", "OldPass123")));
        var newLogin = authService.login(new LoginRequest("hoangnam@gmail.com", "NewPass123"));
        assertEquals(1, jwtUtil.extractTokenVersion(newLogin.getAccessToken()));

        // Verified PHONE user forgot-password -> resetPassword works identically
        User phoneUser = new User();
        phoneUser.setFullName("Đặng Thu");
        phoneUser.setPhone("+84933445566");
        phoneUser.setPassword(passwordEncoder.encode("PhoneOld1"));
        phoneUser.setRole(Role.TENANT);
        phoneUser.setEmailVerified(false);
        phoneUser.setPhoneVerified(true);
        phoneUser = userRepository.save(phoneUser);

        authService.forgotPassword(new ForgotPasswordRequest(AuthVerificationToken.Channel.PHONE, "0933445566"), "127.0.0.1");
        assertEquals(1, sentSms.size());
        var smsResetTicket = authService.verifyResetOtp(
                new VerifyResetOtpRequest(AuthVerificationToken.Channel.PHONE, "+84933445566", sentSms.get(0).otpOrNull()));
        authService.resetPassword(new ResetPasswordRequest(
                AuthVerificationToken.Channel.PHONE, "0933445566", smsResetTicket.resetToken(), "PhoneNew2", "PhoneNew2"));
        assertNotNull(authService.login(new LoginRequest("0933445566", "PhoneNew2")).getAccessToken());
    }

    @Test
    void profileContactLinking_linksSecondaryChannelViaOtpAndBlocksDirectPhoneChange() {
        User emailUser = new User();
        emailUser.setFullName("Vũ Hà");
        emailUser.setEmail("vuha@gmail.com");
        emailUser.setPassword(passwordEncoder.encode("Secret123"));
        emailUser.setRole(Role.TENANT);
        emailUser.setEmailVerified(true);
        emailUser.setPhoneVerified(false);
        emailUser = userRepository.save(emailUser);

        // Direct update with a different phone without OTP must be rejected
        User finalEmailUser = emailUser;
        assertThrows(RuntimeException.class, () -> userProfileService.update(
                finalEmailUser, new UpdateProfileRequest("Vũ Hà Updated", "0911223344")));

        // Direct name-only update succeeds
        ProfileResponse renamed = userProfileService.update(emailUser, new UpdateProfileRequest("Vũ Hà Updated", null));
        assertEquals("Vũ Hà Updated", renamed.fullName());

        // Link Vietnam phone via OTP
        userProfileService.requestContactOtp(
                emailUser, new RequestContactOtpRequest(AuthVerificationToken.Channel.PHONE, "0911223344"), "127.0.0.1");
        assertEquals(1, sentSms.size());
        ProfileResponse linked = userProfileService.verifyContactOtp(
                emailUser, new VerifyContactOtpRequest(AuthVerificationToken.Channel.PHONE, "0911223344", sentSms.get(0).otpOrNull()));
        assertEquals("+84911223344", linked.phone());
        assertTrue(linked.phoneVerified());
        assertTrue(linked.emailVerified());
    }

    @Test
    void missingProviderCredentials_throws503AndLegacyVerifiedAccountsRemainFunctional() {
        GmailEmailService unconfiguredMail = new GmailEmailService(
                new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class),
                "", "", "", "RoomCare"
        );
        TwilioSmsOtpProvider unconfiguredSms = new TwilioSmsOtpProvider(
                "", "", "", "", "", 5, objectMapper
        );
        assertFalse(unconfiguredMail.isConfigured());
        assertFalse(unconfiguredSms.isConfigured());

        assertThrows(MailDeliveryException.class, () -> unconfiguredMail.sendRegistrationVerificationEmail(
                "user@gmail.com", "Test", "123456", 5, "http://localhost"));
        assertThrows(SmsDeliveryException.class, () -> unconfiguredSms.sendOtp(
                "+84987654321", "123456", "dang ky", 5));

        // Legacy seeded user with pre-existing email + phone verified still logs in
        User legacy = new User();
        legacy.setFullName("Legacy Landlord");
        legacy.setEmail("landlord1@example.com");
        legacy.setPhone("0901234567");
        legacy.setPassword(passwordEncoder.encode("123456"));
        legacy.setRole(Role.LANDLORD);
        legacy.setEmailVerified(true);
        legacy.setPhoneVerified(true);
        legacy = userRepository.save(legacy);

        var emailLogin = authService.login(new LoginRequest("landlord1@example.com", "123456"));
        var phoneLogin = authService.login(new LoginRequest("0901234567", "123456"));
        assertNotNull(emailLogin.getAccessToken());
        assertNotNull(phoneLogin.getAccessToken());
    }

    @Test
    void mockMvcAuthEndpoints_enforce403Unverified_429RateLimit_503MissingProvider_and200Flows() throws Exception {
        // 1. Register via Gmail through HTTP
        RegisterRequest registerReq = new RegisterRequest("HTTP Tester", "httptest@gmail.com", null, "Pass123", "TENANT");
        registerReq.setVerificationMethod("EMAIL");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requiresVerification").value(true))
                .andExpect(jsonPath("$.data.verificationChannel").value("EMAIL"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());

        // 2. Unverified login -> 403 Forbidden
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("httptest@gmail.com", "Pass123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.verificationChannel").value("EMAIL"));

        // 3. Immediate resend -> 429 Too Many Requests
        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResendOtpRequest(AuthVerificationToken.Channel.EMAIL, "httptest@gmail.com"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));

        // 4. Verify OTP -> 200 OK, then Login -> 200 OK with JWT
        String otp = sentEmails.get(0).otp();
        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyOtpRequest(AuthVerificationToken.Channel.EMAIL, "httptest@gmail.com", otp))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("httptest@gmail.com", "Pass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.emailVerified").value(true));

        // 5. Forgot password anti-enumeration -> 200 OK
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest(AuthVerificationToken.Channel.EMAIL, "unknown.user@gmail.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 6. Missing email/SMS provider returns 503 Service Unavailable
        emailService = new GmailEmailService(
                new StaticListableBeanFactory().getBeanProvider(JavaMailSender.class),
                "", "", "", "RoomCare"
        );
        initServices(5, 60, 5, 20, 5);
        RegisterRequest noSmtpReq = new RegisterRequest("No SMTP", "nosmtp@gmail.com", null, "Pass123", "TENANT");
        noSmtpReq.setVerificationMethod("EMAIL");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noSmtpReq)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
    }
}
