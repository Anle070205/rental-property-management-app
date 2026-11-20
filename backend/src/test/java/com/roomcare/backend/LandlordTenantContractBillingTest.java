package com.roomcare.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomcare.backend.auth.AuthController;
import com.roomcare.backend.auth.AuthService;
import com.roomcare.backend.controller.TenantController;
import com.roomcare.backend.controller.TenantPortalController;
import com.roomcare.backend.dto.ActivateAccountRequest;
import com.roomcare.backend.dto.LoginRequest;
import com.roomcare.backend.dto.contract.CreateContractRequest;
import com.roomcare.backend.dto.contract.ContractResponse;
import com.roomcare.backend.dto.invoice.AdjustInvoiceRequest;
import com.roomcare.backend.dto.invoice.CancelInvoiceRequest;
import com.roomcare.backend.dto.invoice.CreateInvoiceRequest;
import com.roomcare.backend.dto.invoice.CreatePaymentRequest;
import com.roomcare.backend.dto.invoice.InvoiceResponse;
import com.roomcare.backend.dto.roommate.CreateRoommateRequest;
import com.roomcare.backend.dto.roommate.RoommateResponse;
import com.roomcare.backend.dto.tenant.ProvisionTenantAccountRequest;
import com.roomcare.backend.dto.tenant.ProvisionTenantAccountResponse;
import com.roomcare.backend.dto.tenant.SubmitPaymentProofRequest;
import com.roomcare.backend.dto.tenant.TenantInvoiceResponse;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.exception.GlobalExceptionHandler;
import com.roomcare.backend.exception.MailDeliveryException;
import com.roomcare.backend.repository.*;
import com.roomcare.backend.security.JwtUtil;
import com.roomcare.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LandlordTenantContractBillingTest {

    private UserRepository userRepository;
    private PropertyRepository propertyRepository;
    private RoomRepository roomRepository;
    private TenantRepository tenantRepository;
    private ContractRepository contractRepository;
    private RoommateRepository roommateRepository;
    private InvoiceRepository invoiceRepository;
    private PaymentRepository paymentRepository;
    private UtilityRecordRepository utilityRecordRepository;
    private BookingRequestRepository bookingRequestRepository;
    private AuthVerificationTokenRepository tokenRepository;
    private NotificationService notificationService;

    private ContractService contractService;
    private RoommateService roommateService;
    private TenantAccountService tenantAccountService;
    private InvoiceService invoiceService;
    private TenantPortalProviderImpl tenantPortalProvider;
    private TenantPortalService tenantPortalService;
    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    private final Map<Long, User> userStore = new LinkedHashMap<>();
    private final Map<Long, Property> propertyStore = new LinkedHashMap<>();
    private final Map<Long, Room> roomStore = new LinkedHashMap<>();
    private final Map<Long, Tenant> tenantStore = new LinkedHashMap<>();
    private final Map<Long, Contract> contractStore = new LinkedHashMap<>();
    private final Map<Long, Roommate> roommateStore = new LinkedHashMap<>();
    private final Map<Long, Invoice> invoiceStore = new LinkedHashMap<>();
    private final Map<Long, Payment> paymentStore = new LinkedHashMap<>();
    private final Map<Long, UtilityRecord> utilityStore = new LinkedHashMap<>();
    private final Map<Long, AuthVerificationToken> tokenStore = new LinkedHashMap<>();
    private final List<String> sentActivationOtps = new ArrayList<>();
    private final List<String> sentSmsOtps = new ArrayList<>();
    private final AtomicLong seq = new AtomicLong(1000L);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean mailConfigured = true;
    private User landlord;
    private User otherLandlord;
    private Property property;
    private Room room101;
    private Room room102;
    private Tenant tenantA;
    private Tenant tenantB;
    private User principalUser;

    @BeforeEach
    void setUp() {
        userStore.clear();
        propertyStore.clear();
        roomStore.clear();
        tenantStore.clear();
        contractStore.clear();
        roommateStore.clear();
        invoiceStore.clear();
        paymentStore.clear();
        utilityStore.clear();
        tokenStore.clear();
        sentActivationOtps.clear();
        sentSmsOtps.clear();
        mailConfigured = true;

        userRepository = mock(UserRepository.class, withSettings().lenient());
        propertyRepository = mock(PropertyRepository.class, withSettings().lenient());
        roomRepository = mock(RoomRepository.class, withSettings().lenient());
        tenantRepository = mock(TenantRepository.class, withSettings().lenient());
        contractRepository = mock(ContractRepository.class, withSettings().lenient());
        roommateRepository = mock(RoommateRepository.class, withSettings().lenient());
        invoiceRepository = mock(InvoiceRepository.class, withSettings().lenient());
        paymentRepository = mock(PaymentRepository.class, withSettings().lenient());
        utilityRecordRepository = mock(UtilityRecordRepository.class, withSettings().lenient());
        bookingRequestRepository = mock(BookingRequestRepository.class, withSettings().lenient());
        tokenRepository = mock(AuthVerificationTokenRepository.class, withSettings().lenient());
        notificationService = mock(NotificationService.class, withSettings().lenient());
        passwordEncoder = new BCryptPasswordEncoder();

        wireStores();

        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "roomcare-super-secret-jwt-key-for-testing-2026-hmac256");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        EmailService emailService = new EmailService() {
            @Override
            public boolean isConfigured() { return mailConfigured; }

            @Override
            public void sendRegistrationVerificationEmail(String toEmail, String fullName, String otpCode, int expirationMinutes, String verificationUrl) {
                if (!mailConfigured) throw new MailDeliveryException("Chưa cấu hình SMTP Gmail trên máy chủ");
                sentActivationOtps.add(otpCode);
            }

            @Override
            public void sendPasswordResetEmail(String toEmail, String fullName, String otpCode, int expirationMinutes) {}

            @Override
            public void sendContactVerificationEmail(String toEmail, String fullName, String otpCode, int expirationMinutes) {}

            @Override
            public void sendAccountActivationEmail(String toEmail, String fullName, String landlordName, String roomCode, String otpCode, int expirationMinutes) {
                if (!mailConfigured) throw new MailDeliveryException("Chưa cấu hình SMTP Gmail trên máy chủ");
                sentActivationOtps.add(otpCode);
            }
        };

        SmsOtpProvider smsProvider = new SmsOtpProvider() {
            @Override public boolean isConfigured() { return false; }
            @Override public boolean isProviderManagedVerification() { return false; }
            @Override public String providerName() { return "NONE"; }
            @Override public String sendOtp(String e164Phone, String otpCodeOrNull, String purposeLabel, int expirationMinutes) {
                throw new com.roomcare.backend.exception.SmsDeliveryException("Chưa cấu hình SMS OTP trên máy chủ");
            }
            @Override public boolean verifyProviderOtp(String e164Phone, String otpCode) { return false; }
        };

        OtpAttemptService otpAttemptService = new OtpAttemptService(tokenRepository);
        OtpVerificationService otpVerificationService = new OtpVerificationService(
                tokenRepository, otpAttemptService, smsProvider, 5, 0, 20, 50, 5, "test-pepper");

        authService = new AuthService(
                userRepository, propertyRepository, tenantRepository,
                passwordEncoder, jwtUtil, otpVerificationService, emailService, smsProvider);

        contractService = new ContractService(
                contractRepository, roomRepository, tenantRepository, userRepository, roommateRepository, notificationService);

        roommateService = new RoommateService(
                roommateRepository, tenantRepository, roomRepository, contractRepository, userRepository);

        tenantAccountService = new TenantAccountService(
                tenantRepository, userRepository, passwordEncoder, otpVerificationService, emailService, smsProvider);
        ReflectionTestUtils.setField(tenantAccountService, "contractRepository", contractRepository);
        ReflectionTestUtils.setField(tenantAccountService, "notificationService", notificationService);

        invoiceService = new InvoiceService(
                invoiceRepository, paymentRepository, roomRepository, tenantRepository,
                userRepository, utilityRecordRepository, notificationService);
        ReflectionTestUtils.setField(invoiceService, "contractRepository", contractRepository);
        ReflectionTestUtils.setField(invoiceService, "roommateRepository", roommateRepository);

        tenantPortalProvider = new TenantPortalProviderImpl(
                invoiceRepository, paymentRepository, roomRepository, bookingRequestRepository, notificationService);
        ReflectionTestUtils.setField(tenantPortalProvider, "contractRepository", contractRepository);
        ReflectionTestUtils.setField(tenantPortalProvider, "roommateRepository", roommateRepository);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("tenantPortalProvider", tenantPortalProvider);
        tenantPortalService = new TenantPortalService(
                tenantRepository, notificationService, beanFactory.getBeanProvider(TenantPortalProvider.class));

        seedFixtures();
    }

    private void seedFixtures() {
        landlord = new User();
        landlord.setId(1L);
        landlord.setFullName("Chủ Trọ A");
        landlord.setEmail("chutro.a@gmail.com");
        landlord.setRole(Role.LANDLORD);
        landlord.setIsActive(true);
        landlord.setEmailVerified(true);
        userStore.put(1L, landlord);

        otherLandlord = new User();
        otherLandlord.setId(2L);
        otherLandlord.setFullName("Chủ Trọ B");
        otherLandlord.setEmail("chutro.b@gmail.com");
        otherLandlord.setRole(Role.LANDLORD);
        otherLandlord.setIsActive(true);
        otherLandlord.setEmailVerified(true);
        userStore.put(2L, otherLandlord);

        property = new Property();
        property.setId(10L);
        property.setLandlord(landlord);
        property.setName("Nhà Trọ An Phúc");
        property.setAddress("123 Nguyễn Trãi");
        propertyStore.put(10L, property);

        room101 = new Room();
        room101.setId(101L);
        room101.setProperty(property);
        room101.setRoomCode("P101");
        room101.setFloor(1);
        room101.setArea(new BigDecimal("25"));
        room101.setRentPrice(new BigDecimal("2120000"));
        room101.setDeposit(new BigDecimal("2120000"));
        room101.setCapacity(3);
        room101.setElectricityPrice(new BigDecimal("3500"));
        room101.setWaterPrice(new BigDecimal("15000"));
        room101.setInternetFee(new BigDecimal("100000"));
        room101.setServiceFee(new BigDecimal("50000"));
        room101.setStatus(RoomStatus.VACANT);
        room101.setHealthScore(100);
        roomStore.put(101L, room101);

        room102 = new Room();
        room102.setId(102L);
        room102.setProperty(property);
        room102.setRoomCode("P102");
        room102.setFloor(1);
        room102.setArea(new BigDecimal("22"));
        room102.setRentPrice(new BigDecimal("2000000"));
        room102.setDeposit(new BigDecimal("2000000"));
        room102.setCapacity(2);
        room102.setElectricityPrice(new BigDecimal("3500"));
        room102.setWaterPrice(new BigDecimal("100000"));
        room102.setStatus(RoomStatus.VACANT);
        room102.setHealthScore(100);
        roomStore.put(102L, room102);

        tenantA = new Tenant();
        tenantA.setId(501L);
        tenantA.setLandlord(landlord);
        tenantA.setProperty(property);
        tenantA.setFullName("Khách Thuê An");
        tenantA.setEmail("khach.an@gmail.com");
        tenantA.setPhone("0909111222");
        tenantStore.put(501L, tenantA);

        tenantB = new Tenant();
        tenantB.setId(502L);
        tenantB.setLandlord(landlord);
        tenantB.setProperty(property);
        tenantB.setFullName("Khách Thuê Bình");
        tenantB.setEmail("khach.binh@gmail.com");
        tenantB.setPhone("0909333444");
        tenantStore.put(502L, tenantB);
    }

    @Test
    void contractLifecycleAndOverlapAndTerminateUpdatesRoomStatus() {
        CreateContractRequest req = new CreateContractRequest();
        req.setRoomId(101L);
        req.setTenantId(501L);
        req.setStartDate(LocalDate.now().withDayOfMonth(1));
        req.setEndDate(LocalDate.now().plusYears(1));
        req.setMonthlyRent(new BigDecimal("2120000"));
        req.setGarbageFee(new BigDecimal("30000"));

        ContractResponse draft = contractService.createContract(1L, req);
        assertEquals(ContractStatus.DRAFT, draft.getStatus());
        assertEquals(RoomStatus.VACANT, room101.getStatus());

        // Chưa ACTIVE thì không được phép lập hóa đơn
        CreateInvoiceRequest invReq = new CreateInvoiceRequest();
        invReq.setRoomId(101L);
        invReq.setBillingMonth(YearMonth.now().toString());
        invReq.setDueDate(LocalDate.now().plusDays(5));
        assertThrows(RuntimeException.class, () -> invoiceService.createInvoice(1L, invReq));

        ContractResponse signed = contractService.signContract(1L, draft.getId(), null);
        assertEquals(ContractStatus.SIGNED, signed.getStatus());
        assertEquals(RoomStatus.VACANT, room101.getStatus());

        ContractResponse active = contractService.activateContract(1L, draft.getId());
        assertEquals(ContractStatus.ACTIVE, active.getStatus());
        assertEquals(RoomStatus.OCCUPIED, room101.getStatus());

        // Chặn tạo hợp đồng trùng khoảng thời gian trên cùng phòng
        CreateContractRequest overlap = new CreateContractRequest();
        overlap.setRoomId(101L);
        overlap.setTenantId(502L);
        overlap.setStartDate(LocalDate.now().plusMonths(1));
        overlap.setEndDate(LocalDate.now().plusMonths(6));
        overlap.setMonthlyRent(new BigDecimal("2200000"));
        assertThrows(RuntimeException.class, () -> contractService.createContract(1L, overlap));

        // Chặn chủ trọ khác xem/thanh lý hợp đồng của Chủ Trọ A
        assertThrows(RuntimeException.class, () -> contractService.getContractById(2L, draft.getId()));

        ContractResponse terminated = contractService.terminateContract(1L, draft.getId(), null);
        assertEquals(ContractStatus.TERMINATED, terminated.getStatus());
        assertEquals(RoomStatus.VACANT, room101.getStatus());
    }

    @Test
    void roommateRequiresCccdConsentAndNeverGetsAutoPortalAccess() {
        ContractResponse c = createActiveContract(101L, 501L, WaterBillingMode.PER_PERSON);

        CreateRoommateRequest noConsent = new CreateRoommateRequest();
        noConsent.setTenantId(501L);
        noConsent.setRoomId(101L);
        noConsent.setContractId(c.getId());
        noConsent.setFullName("Nguyễn Văn Ở Cùng");
        noConsent.setIdentityNumber("079200009999");
        noConsent.setConsentConfirmed(false);
        assertThrows(RuntimeException.class, () -> roommateService.addRoommate(1L, noConsent));

        noConsent.setConsentConfirmed(true);
        noConsent.setHasPortalAccess(false);
        RoommateResponse saved = roommateService.addRoommate(1L, noConsent);
        assertFalse(saved.getIsRepresentative());
        assertFalse(saved.getHasPortalAccess());
        assertTrue(saved.getConsentConfirmed());
        assertEquals("********9999", saved.getMaskedIdentityNumber());
    }

    @Test
    void provisionAccountHonest503WhenUnconfiguredAndOtpActivationWorksEndToEnd() throws Exception {
        MockMvc mvc = buildMockMvc();

        // 1. Khi chưa cấu hình SMTP -> trả 503 thật, tuyệt đối không giả lập thành công
        mailConfigured = false;
        ProvisionTenantAccountRequest pReq = new ProvisionTenantAccountRequest(
                AuthVerificationToken.Channel.EMAIL, "khach.an@gmail.com", false);
        principalUser = landlord;
        mvc.perform(post("/api/landlord/tenants/501/provision-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pReq)))
                .andExpect(status().isServiceUnavailable());

        // 2. Khi SMTP khả dụng -> tạo User PENDING_ACTIVATION, lưu OTP hash và gửi OTP kích hoạt
        // Hết thời gian chờ cooldown để gửi lại hợp lệ
        tokenStore.values().forEach(t -> t.setCreatedAt(t.getCreatedAt().minusMinutes(2)));
        mailConfigured = true;
        ProvisionTenantAccountResponse prov = tenantAccountService.provisionOrLinkAccount(1L, 501L, pReq, "127.0.0.1");
        assertEquals("PENDING_ACTIVATION", prov.getAccountStatus());
        assertEquals(1, sentActivationOtps.size());
        String rawOtp = sentActivationOtps.get(0);

        User tenantUser = tenantA.getUser();
        assertNotNull(tenantUser);
        assertEquals(Role.TENANT, tenantUser.getRole());
        assertFalse(tenantUser.isAccountVerified());

        // Không có OTP thì chưa đăng nhập được
        assertThrows(RuntimeException.class, () ->
                authService.login(new LoginRequest("khach.an@gmail.com", "MatKhauMoi123")));

        // 3. Khách dùng OTP kích hoạt & tự đặt mật khẩu riêng
        ActivateAccountRequest activateReq = new ActivateAccountRequest(
                AuthVerificationToken.Channel.EMAIL, "khach.an@gmail.com", rawOtp, "MatKhauMoi123", "MatKhauMoi123");
        mvc.perform(post("/api/auth/activate-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertTrue(tenantUser.isAccountVerified());
        var loginRes = authService.login(new LoginRequest("khach.an@gmail.com", "MatKhauMoi123"));
        assertEquals("TENANT", loginRes.getRole());
        assertNotNull(loginRes.getAccessToken());

        // 4. Khi user đã tồn tại nhưng chưa tick confirmLinkExisting -> từ chối liên kết mù
        Tenant tenantBNoLink = tenantB;
        ProvisionTenantAccountRequest linkReq = new ProvisionTenantAccountRequest(
                AuthVerificationToken.Channel.EMAIL, "khach.an@gmail.com", false);
        assertThrows(RuntimeException.class, () ->
                tenantAccountService.provisionOrLinkAccount(1L, tenantBNoLink.getId(), linkReq, "127.0.0.1"));
    }

    @Test
    void fullInvoiceLifecycleDraftPublishProofPaymentAndCrossTenantIsolation() throws Exception {
        // Kích hoạt User cho cả Tenant A và Tenant B
        provisionAndActivate(tenantA, "khach.an@gmail.com");
        provisionAndActivate(tenantB, "khach.binh@gmail.com");

        ContractResponse activeContract = createActiveContract(101L, 501L, WaterBillingMode.METERED);
        CreateContractRequest cReqB = new CreateContractRequest();
        cReqB.setRoomId(102L);
        cReqB.setTenantId(502L);
        cReqB.setStartDate(LocalDate.now().withDayOfMonth(1));
        cReqB.setEndDate(LocalDate.now().plusYears(1));
        cReqB.setMonthlyRent(new BigDecimal("2000000"));
        cReqB.setWaterBillingMode(WaterBillingMode.FIXED);
        ContractResponse draftB = contractService.createContract(1L, cReqB);
        contractService.signContract(1L, draftB.getId(), null);
        contractService.activateContract(1L, draftB.getId());

        String month = YearMonth.now().toString();
        CreateInvoiceRequest draftReq = new CreateInvoiceRequest();
        draftReq.setRoomId(101L);
        draftReq.setContractId(activeContract.getId());
        draftReq.setBillingMonth(month);
        draftReq.setDueDate(LocalDate.now().plusDays(7));
        draftReq.setPrevElectricIndex(new BigDecimal("100"));
        draftReq.setCurrElectricIndex(new BigDecimal("150"));
        draftReq.setPrevWaterIndex(new BigDecimal("10"));
        draftReq.setCurrWaterIndex(new BigDecimal("15"));
        draftReq.setGarbageFee(new BigDecimal("30000"));
        draftReq.setSaveAsDraft(true);

        // Preview kiểm tra công thức: 2120000 + 50*3500(175000) + 5*15000(75000) + 100000 + 30000 + 50000 = 2550000
        InvoiceResponse preview = invoiceService.previewInvoice(1L, draftReq);
        assertEquals(new BigDecimal("2550000"), preview.getTotalAmount());
        assertEquals(InvoiceStatus.DRAFT, preview.getStatus());

        // Tạo DRAFT -> khách thuê KHÔNG được thấy
        InvoiceResponse savedDraft = invoiceService.createInvoice(1L, draftReq);
        assertEquals(InvoiceStatus.DRAFT, savedDraft.getStatus());

        MockMvc mvc = buildMockMvc();
        principalUser = tenantA.getUser();
        mvc.perform(get("/api/tenant/invoices/" + savedDraft.getId()))
                .andExpect(status().isNotFound());
        assertEquals(0, tenantPortalService.invoices(tenantA.getUser(), null, null, 0, 20).getContent().size());

        // Không được thu tiền khi còn DRAFT
        CreatePaymentRequest payAttempt = new CreatePaymentRequest(
                new BigDecimal("2550000"), PaymentMethod.BANK_TRANSFER, "TX-DRAFT", "Early", true);
        assertThrows(RuntimeException.class, () -> invoiceService.recordPayment(1L, savedDraft.getId(), payAttempt));

        // Publish -> status UNPAID, Tenant A xem được đầy đủ chi tiết
        InvoiceResponse published = invoiceService.publishInvoice(1L, savedDraft.getId());
        assertEquals(InvoiceStatus.UNPAID, published.getStatus());
        assertNotNull(published.getPublishedAt());

        TenantInvoiceResponse tenantView = tenantPortalService.invoice(tenantA.getUser(), published.getId()).orElseThrow();
        assertEquals(new BigDecimal("2550000"), tenantView.totalAmount());
        assertEquals(new BigDecimal("30000"), tenantView.garbageFee());

        // Tenant B không thể xem hóa đơn hoặc gửi minh chứng thay Tenant A (404)
        principalUser = tenantB.getUser();
        mvc.perform(get("/api/tenant/invoices/" + published.getId()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/tenant/invoices/" + published.getId() + "/payment-proof")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitPaymentProofRequest("Hack", null))))
                .andExpect(status().isNotFound());

        // Không được lập trùng hóa đơn cùng phòng + kỳ
        assertThrows(RuntimeException.class, () -> invoiceService.createInvoice(1L, draftReq));

        // Điều chỉnh +50.000đ có audit log
        AdjustInvoiceRequest adj = new AdjustInvoiceRequest();
        adj.setAdjustmentAmount(new BigDecimal("50000"));
        adj.setAdjustmentReason("Phụ thu gửi xe máy");
        InvoiceResponse adjusted = invoiceService.adjustInvoice(1L, published.getId(), adj);
        assertEquals(new BigDecimal("2600000"), adjusted.getTotalAmount());
        assertTrue(adjusted.getAdjustmentHistory().contains("Phụ thu gửi xe máy"));

        // Tenant A gửi minh chứng -> trạng thái VẪN LÀ UNPAID, không tự thành PAID
        principalUser = tenantA.getUser();
        mvc.perform(post("/api/tenant/invoices/" + published.getId() + "/payment-proof")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitPaymentProofRequest("CK VCB FT123456", "https://example.com/bill.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNPAID"))
                .andExpect(jsonPath("$.data.paymentProofNote").value("CK VCB FT123456"));

        // Chủ nhà xác nhận thực thu -> hóa đơn thành PAID, đồng bộ cả 2 phía
        CreatePaymentRequest confirmPay = new CreatePaymentRequest(
                new BigDecimal("2600000"), PaymentMethod.BANK_TRANSFER, "FT123456", "Đã nhận VCB", true);
        InvoiceResponse paid = invoiceService.recordPayment(1L, published.getId(), confirmPay);
        assertEquals(InvoiceStatus.PAID, paid.getStatus());
        assertEquals(BigDecimal.ZERO, paid.getRemainingAmount());
        assertEquals("PAID", tenantPortalService.invoice(tenantA.getUser(), published.getId()).orElseThrow().status());

        // Khóa chỉnh sửa và hủy sau khi đã PAID
        assertThrows(RuntimeException.class, () -> invoiceService.adjustInvoice(1L, paid.getId(), adj));
        assertThrows(RuntimeException.class, () -> invoiceService.cancelInvoice(1L, paid.getId(), new CancelInvoiceRequest("Thử hủy")));

        // Hủy hóa đơn chưa thanh toán trên phòng 102 và lập lại kỳ đó được phép
        CreateInvoiceRequest req102 = new CreateInvoiceRequest();
        req102.setRoomId(102L);
        req102.setContractId(draftB.getId());
        req102.setBillingMonth(month);
        req102.setDueDate(LocalDate.now().plusDays(5));
        InvoiceResponse inv102 = invoiceService.createInvoice(1L, req102);
        InvoiceResponse cancelled102 = invoiceService.cancelInvoice(1L, inv102.getId(), new CancelInvoiceRequest("Nhập sai tiền nước"));
        assertEquals(InvoiceStatus.CANCELLED, cancelled102.getStatus());

        InvoiceResponse recreated102 = invoiceService.createInvoice(1L, req102);
        assertEquals(InvoiceStatus.UNPAID, recreated102.getStatus());
        assertNotEquals(inv102.getInvoiceCode(), recreated102.getInvoiceCode());
    }

    private ContractResponse createActiveContract(Long roomId, Long tenantId, WaterBillingMode waterMode) {
        CreateContractRequest req = new CreateContractRequest();
        req.setRoomId(roomId);
        req.setTenantId(tenantId);
        req.setStartDate(LocalDate.now().withDayOfMonth(1));
        req.setEndDate(LocalDate.now().plusYears(1));
        req.setMonthlyRent(new BigDecimal("2120000"));
        req.setGarbageFee(new BigDecimal("30000"));
        req.setWaterBillingMode(waterMode);
        ContractResponse draft = contractService.createContract(1L, req);
        contractService.signContract(1L, draft.getId(), null);
        return contractService.activateContract(1L, draft.getId());
    }

    private void provisionAndActivate(Tenant t, String email) {
        sentActivationOtps.clear();
        tenantAccountService.provisionOrLinkAccount(
                1L, t.getId(), new ProvisionTenantAccountRequest(AuthVerificationToken.Channel.EMAIL, email, false), "127.0.0.1");
        String otp = sentActivationOtps.get(sentActivationOtps.size() - 1);
        tenantAccountService.activateAccount(
                new ActivateAccountRequest(AuthVerificationToken.Channel.EMAIL, email, otp, "TenantPass123", "TenantPass123"));
    }

    private MockMvc buildMockMvc() {
        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(User.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return principalUser;
            }
        };
        return MockMvcBuilders.standaloneSetup(
                        new AuthController(authService) {{ ReflectionTestUtils.setField(this, "tenantAccountService", tenantAccountService); }},
                        new TenantController(mock(TenantService.class)) {{ ReflectionTestUtils.setField(this, "tenantAccountService", tenantAccountService); }},
                        new TenantPortalController(tenantPortalService))
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void wireStores() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(seq.incrementAndGet());
            userStore.put(u.getId(), u);
            return u;
        });
        when(userRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(userStore.get(inv.getArgument(0))));
        when(userRepository.findByEmailIgnoreCase(anyString())).thenAnswer(inv -> {
            String e = inv.getArgument(0);
            return userStore.values().stream().filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(e)).findFirst();
        });
        when(userRepository.findFirstByPhoneIn(anyCollection())).thenAnswer(inv -> {
            Collection<String> phones = inv.getArgument(0);
            return userStore.values().stream().filter(u -> u.getPhone() != null && phones.contains(u.getPhone())).findFirst();
        });

        when(roomRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(roomStore.get(inv.getArgument(0))));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            roomStore.put(r.getId(), r);
            return r;
        });

        when(tenantRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(tenantStore.get(inv.getArgument(0))));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(seq.incrementAndGet());
            tenantStore.put(t.getId(), t);
            return t;
        });
        when(tenantRepository.findByUserId(anyLong())).thenAnswer(inv -> {
            Long uid = inv.getArgument(0);
            return tenantStore.values().stream().filter(t -> t.getUser() != null && Objects.equals(t.getUser().getId(), uid)).findFirst();
        });
        when(tenantRepository.findFirstByEmailIgnoreCaseOrPhone(any(), any())).thenAnswer(inv -> {
            String em = inv.getArgument(0);
            String ph = inv.getArgument(1);
            return tenantStore.values().stream().filter(t ->
                    (em != null && t.getEmail() != null && t.getEmail().equalsIgnoreCase(em))
                            || (ph != null && Objects.equals(t.getPhone(), ph))).findFirst();
        });

        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            if (c.getId() == null) c.setId(seq.incrementAndGet());
            if (c.getCreatedAt() == null) c.setCreatedAt(LocalDateTime.now());
            contractStore.put(c.getId(), c);
            return c;
        });
        when(contractRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(contractStore.get(inv.getArgument(0))));
        when(contractRepository.findByRoomIdOrderByStartDateDescCreatedAtDesc(anyLong())).thenAnswer(inv -> {
            Long rid = inv.getArgument(0);
            return contractStore.values().stream().filter(c -> Objects.equals(c.getRoom().getId(), rid)).collect(Collectors.toList());
        });
        when(contractRepository.findFirstByRoomIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(anyLong(), anyCollection())).thenAnswer(inv -> {
            Long rid = inv.getArgument(0);
            Collection<ContractStatus> sts = inv.getArgument(1);
            return contractStore.values().stream()
                    .filter(c -> Objects.equals(c.getRoom().getId(), rid) && sts.contains(c.getStatus()))
                    .findFirst();
        });
        when(contractRepository.findFirstByTenantIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(anyLong(), anyCollection())).thenAnswer(inv -> {
            Long tid = inv.getArgument(0);
            Collection<ContractStatus> sts = inv.getArgument(1);
            return contractStore.values().stream()
                    .filter(c -> Objects.equals(c.getTenant().getId(), tid) && sts.contains(c.getStatus()))
                    .findFirst();
        });
        when(contractRepository.findOverlappingContracts(anyLong(), anyCollection(), any(), any(), any())).thenAnswer(inv -> {
            Long rid = inv.getArgument(0);
            Collection<ContractStatus> sts = inv.getArgument(1);
            LocalDate start = inv.getArgument(2);
            LocalDate end = inv.getArgument(3);
            Long excludeId = inv.getArgument(4);
            return contractStore.values().stream()
                    .filter(c -> Objects.equals(c.getRoom().getId(), rid))
                    .filter(c -> sts.contains(c.getStatus()))
                    .filter(c -> excludeId == null || !Objects.equals(c.getId(), excludeId))
                    .filter(c -> !c.getStartDate().isAfter(end) && !c.getEndDate().isBefore(start))
                    .collect(Collectors.toList());
        });

        when(roommateRepository.save(any(Roommate.class))).thenAnswer(inv -> {
            Roommate r = inv.getArgument(0);
            if (r.getId() == null) r.setId(seq.incrementAndGet());
            roommateStore.put(r.getId(), r);
            return r;
        });
        when(roommateRepository.findByContractIdOrderByCreatedAtAsc(anyLong())).thenAnswer(inv -> {
            Long cid = inv.getArgument(0);
            return roommateStore.values().stream()
                    .filter(r -> r.getContract() != null && Objects.equals(r.getContract().getId(), cid))
                    .collect(Collectors.toList());
        });
        when(roommateRepository.countByContractId(anyLong())).thenAnswer(inv -> {
            Long cid = inv.getArgument(0);
            return roommateStore.values().stream()
                    .filter(r -> r.getContract() != null && Objects.equals(r.getContract().getId(), cid))
                    .count();
        });

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId(seq.incrementAndGet());
            if (i.getCreatedAt() == null) i.setCreatedAt(LocalDateTime.now());
            invoiceStore.put(i.getId(), i);
            return i;
        });
        when(invoiceRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(invoiceStore.get(inv.getArgument(0))));
        when(invoiceRepository.findAllByRoomIdAndBillingMonthOrderByCreatedAtDesc(anyLong(), anyString())).thenAnswer(inv -> {
            Long rid = inv.getArgument(0);
            String m = inv.getArgument(1);
            return invoiceStore.values().stream()
                    .filter(i -> Objects.equals(i.getRoom().getId(), rid) && Objects.equals(i.getBillingMonth(), m))
                    .collect(Collectors.toList());
        });
        when(invoiceRepository.findByTenantIdOrderByBillingMonthDescCreatedAtDesc(anyLong())).thenAnswer(inv -> {
            Long tid = inv.getArgument(0);
            return invoiceStore.values().stream()
                    .filter(i -> i.getTenant() != null && Objects.equals(i.getTenant().getId(), tid))
                    .collect(Collectors.toList());
        });
        when(invoiceRepository.findByRoomIdOrderByBillingMonthDesc(anyLong())).thenAnswer(inv -> {
            Long rid = inv.getArgument(0);
            return invoiceStore.values().stream().filter(i -> Objects.equals(i.getRoom().getId(), rid)).collect(Collectors.toList());
        });

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(seq.incrementAndGet());
            paymentStore.put(p.getId(), p);
            return p;
        });
        when(paymentRepository.findByInvoiceIdOrderByPaidAtDesc(anyLong())).thenAnswer(inv -> {
            Long iid = inv.getArgument(0);
            return paymentStore.values().stream().filter(p -> Objects.equals(p.getInvoice().getId(), iid)).collect(Collectors.toList());
        });

        Comparator<AuthVerificationToken> latestFirst = Comparator
                .comparing(AuthVerificationToken::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AuthVerificationToken::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed();

        when(tokenRepository.save(any(AuthVerificationToken.class))).thenAnswer(inv -> {
            AuthVerificationToken t = inv.getArgument(0);
            if (t.getId() == null) t.setId(seq.incrementAndGet());
            if (t.getCreatedAt() == null) t.setCreatedAt(LocalDateTime.now());
            tokenStore.put(t.getId(), t);
            return t;
        });
        when(tokenRepository.saveAll(anyIterable())).thenAnswer(inv -> {
            Iterable<AuthVerificationToken> list = inv.getArgument(0);
            List<AuthVerificationToken> res = new ArrayList<>();
            for (AuthVerificationToken t : list) {
                if (t.getId() == null) t.setId(seq.incrementAndGet());
                if (t.getCreatedAt() == null) t.setCreatedAt(LocalDateTime.now());
                tokenStore.put(t.getId(), t);
                res.add(t);
            }
            return res;
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
    }
}
