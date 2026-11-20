package com.roomcare.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomcare.backend.config.SecurityConfig;
import com.roomcare.backend.controller.AdminController;
import com.roomcare.backend.dto.admin.*;
import com.roomcare.backend.dto.user.ChangePasswordRequest;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.exception.GlobalExceptionHandler;
import com.roomcare.backend.repository.*;
import com.roomcare.backend.security.JwtAuthenticationFilter;
import com.roomcare.backend.security.JwtUtil;
import com.roomcare.backend.service.AdminService;
import com.roomcare.backend.service.NotificationService;
import com.roomcare.backend.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminModuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private Map<Long, User> userStore;
    private Map<Long, Property> propertyStore;
    private Map<Long, Room> roomStore;
    private Map<Long, Tenant> tenantStore;
    private Map<Long, Incident> incidentStore;
    private Map<Long, AdminBroadcast> broadcastStore;
    private List<Notification> sentNotifications;

    private User adminUser;
    private User landlordUser;
    private User tenantUser;
    private Property sampleProperty;
    private Incident sampleIncident;

    private AdminService adminService;
    private JwtUtil jwtUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userStore = new LinkedHashMap<>();
        propertyStore = new LinkedHashMap<>();
        roomStore = new LinkedHashMap<>();
        tenantStore = new LinkedHashMap<>();
        incidentStore = new LinkedHashMap<>();
        broadcastStore = new LinkedHashMap<>();
        sentNotifications = new ArrayList<>();

        AtomicLong idGen = new AtomicLong(100);

        UserRepository userRepo = Mockito.mock(UserRepository.class);
        PropertyRepository propRepo = Mockito.mock(PropertyRepository.class);
        RoomRepository roomRepo = Mockito.mock(RoomRepository.class);
        TenantRepository tenantRepo = Mockito.mock(TenantRepository.class);
        ContractRepository contractRepo = Mockito.mock(ContractRepository.class);
        IncidentRepository incidentRepo = Mockito.mock(IncidentRepository.class);
        AdminBroadcastRepository broadcastRepo = Mockito.mock(AdminBroadcastRepository.class);
        NotificationService notificationService = Mockito.mock(NotificationService.class);

        when(userRepo.findAll()).thenAnswer(inv -> new ArrayList<>(userStore.values()));
        when(userRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(userStore.get(inv.<Long>getArgument(0))));
        when(userRepo.existsByEmailIgnoreCase(any())).thenAnswer(inv -> {
            String e = inv.getArgument(0);
            return userStore.values().stream().anyMatch(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(e));
        });
        when(userRepo.findByEmailIgnoreCase(any())).thenAnswer(inv -> {
            String e = inv.getArgument(0);
            return userStore.values().stream().filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(e)).findFirst();
        });
        when(userRepo.existsByPhoneIn(any())).thenReturn(false);
        when(userRepo.findFirstByPhoneIn(any())).thenReturn(Optional.empty());
        when(userRepo.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(idGen.incrementAndGet());
            if (u.getCreatedAt() == null) u.setCreatedAt(LocalDateTime.now());
            userStore.put(u.getId(), u);
            return u;
        });

        when(propRepo.findAll()).thenAnswer(inv -> new ArrayList<>(propertyStore.values()));
        when(propRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(propertyStore.get(inv.<Long>getArgument(0))));
        when(roomRepo.findAll()).thenAnswer(inv -> new ArrayList<>(roomStore.values()));
        when(roomRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(roomStore.get(inv.<Long>getArgument(0))));
        when(tenantRepo.findAll()).thenAnswer(inv -> new ArrayList<>(tenantStore.values()));
        when(tenantRepo.findByUserId(any())).thenAnswer(inv -> {
            Long uid = inv.getArgument(0);
            return tenantStore.values().stream().filter(t -> t.getUser() != null && uid.equals(t.getUser().getId())).findFirst();
        });
        when(tenantRepo.save(any())).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) t.setId(idGen.incrementAndGet());
            tenantStore.put(t.getId(), t);
            return t;
        });

        when(contractRepo.findAll()).thenReturn(List.of());
        when(contractRepo.findFirstByTenantIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(any(), any())).thenReturn(Optional.empty());

        when(incidentRepo.findAll()).thenAnswer(inv -> new ArrayList<>(incidentStore.values()));
        when(incidentRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(incidentStore.get(inv.<Long>getArgument(0))));
        when(incidentRepo.save(any())).thenAnswer(inv -> {
            Incident i = inv.getArgument(0);
            incidentStore.put(i.getId(), i);
            return i;
        });

        when(broadcastRepo.findAllByOrderByUpdatedAtDescIdDesc()).thenAnswer(inv -> new ArrayList<>(broadcastStore.values()));
        when(broadcastRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(broadcastStore.get(inv.<Long>getArgument(0))));
        when(broadcastRepo.count()).thenAnswer(inv -> (long) broadcastStore.size());
        when(broadcastRepo.save(any())).thenAnswer(inv -> {
            AdminBroadcast b = inv.getArgument(0);
            if (b.getId() == null) b.setId(idGen.incrementAndGet());
            if (b.getCreatedAt() == null) b.setCreatedAt(LocalDateTime.now());
            b.setUpdatedAt(LocalDateTime.now());
            broadcastStore.put(b.getId(), b);
            return b;
        });

        doAnswer(inv -> {
            Notification n = new Notification();
            n.setUser(inv.getArgument(0));
            n.setType(inv.getArgument(1));
            n.setTitle(inv.getArgument(2));
            n.setContent(inv.getArgument(3));
            sentNotifications.add(n);
            return null;
        }).when(notificationService).createNotification(any(), any(), any(), any(), any());

        adminUser = new User("Nguyễn Minh Anh", "admin@roomcare.vn", "0908112233", passwordEncoder.encode("Admin@123456"), Role.ADMIN);
        adminUser.setId(1L);
        adminUser.setCreatedAt(LocalDateTime.now());
        userStore.put(1L, adminUser);

        landlordUser = new User("Tài Bình An", "landlord@roomcare.vn", "0901234567", passwordEncoder.encode("123456"), Role.LANDLORD);
        landlordUser.setId(2L);
        landlordUser.setCreatedAt(LocalDateTime.now());
        userStore.put(2L, landlordUser);

        tenantUser = new User("Phạm Gia Bảo", "tenant@roomcare.vn", "0938456210", passwordEncoder.encode("123456"), Role.TENANT);
        tenantUser.setId(3L);
        tenantUser.setCreatedAt(LocalDateTime.now());
        userStore.put(3L, tenantUser);

        sampleProperty = new Property();
        sampleProperty.setId(10L);
        sampleProperty.setLandlord(landlordUser);
        sampleProperty.setName("Nhà trọ An Phúc");
        sampleProperty.setAddress("25 Nguyễn Gia Trí, Bình Thạnh");
        propertyStore.put(10L, sampleProperty);

        Room r1 = new Room();
        r1.setId(20L);
        r1.setProperty(sampleProperty);
        r1.setRoomCode("P.305");
        r1.setFloor(3);
        r1.setStatus(RoomStatus.OCCUPIED);
        roomStore.put(20L, r1);

        Tenant tProfile = new Tenant();
        tProfile.setId(30L);
        tProfile.setLandlord(landlordUser);
        tProfile.setProperty(sampleProperty);
        tProfile.setUser(tenantUser);
        tProfile.setFullName("Phạm Gia Bảo");
        tenantStore.put(30L, tProfile);

        sampleIncident = new Incident();
        sampleIncident.setId(24L);
        sampleIncident.setTenant(tProfile);
        sampleIncident.setRoom(r1);
        sampleIncident.setType(Incident.Type.OTHER);
        sampleIncident.setPriority(Incident.Priority.HIGH);
        sampleIncident.setStatus(Incident.Status.RECEIVED);
        sampleIncident.setTitle("Không truy cập được tài khoản");
        sampleIncident.setDescription("Không nhận được email khôi phục mật khẩu.");
        sampleIncident.setCreatedAt(LocalDateTime.now());
        incidentStore.put(24L, sampleIncident);

        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "RoomCareSecretKeyForJWTTokenGenerationAndValidation2024VeryLongSecretKey");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        UserProfileService profileService = new UserProfileService(userRepo, passwordEncoder);
        adminService = new AdminService(
                userRepo, propRepo, roomRepo, tenantRepo, contractRepo,
                incidentRepo, broadcastRepo, notificationService, profileService, passwordEncoder, jwtUtil
        );

        AdminController controller = new AdminController(adminService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void adminDashboardAndLandlordPropertyMonitoring_returnRealAggregates() {
        AdminDashboardResponse dash = adminService.getDashboard(adminUser);
        assertEquals(3, dash.totalAccounts());
        assertEquals(1, dash.landlordCount());
        assertEquals(1, dash.tenantCount());
        assertEquals(1, dash.adminCount());
        assertEquals(1, dash.propertyCount());
        assertEquals(1, dash.totalRooms());
        assertEquals(1, dash.occupiedRooms());
        assertEquals(6, dash.monthlyGrowth().size());

        AdminLandlordResponse landlordDetail = adminService.getLandlordDetail(2L);
        assertEquals("Tài Bình An", landlordDetail.fullName());
        assertEquals(1, landlordDetail.properties().size());
        assertEquals(1, landlordDetail.tenants().size());

        AdminPropertyResponse propDetail = adminService.getPropertyDetail(10L);
        assertEquals(100, propDetail.occupancyPercent());
    }

    @Test
    void adminAccountLifecycle_preventsSelfLock_andInvalidatesJwtOnLock() {
        assertThrows(RuntimeException.class, () -> adminService.lockAccount(1L, 1L));

        AdminAccountResponse locked = adminService.lockAccount(1L, 3L);
        assertFalse(locked.isActive());
        assertEquals(1, userStore.get(3L).getTokenVersion());

        AdminAccountResponse unlocked = adminService.unlockAccount(3L);
        assertTrue(unlocked.isActive());

        AdminAccountResponse created = adminService.createAccount(new AdminCreateAccountRequest(
                "Nguyễn Đức Huy", "duchuy@gmail.com", "0912550889",
                "TENANT", true, "Secret123", 2L, 10L, null
        ));
        assertEquals("TENANT", created.role());
        assertTrue(created.hasTenantLink());
    }

    @Test
    void adminReportResolutionAndBroadcast_dispatchNotificationsOnlyToActiveUsers() {
        AdminReportResponse resolved = adminService.resolveReport(
                adminUser, 24L, new AdminResolveReportRequest("Đã kiểm tra dịch vụ email.", "DONE")
        );
        assertEquals("DONE", resolved.status());
        assertEquals("Nguyễn Minh Anh", resolved.resolvedByName());
        assertEquals(1, sentNotifications.size());

        sentNotifications.clear();
        // Lock tenantUser -> should be excluded from broadcast
        adminService.lockAccount(1L, 3L);

        AdminBroadcastResponse draft = adminService.createBroadcast(
                adminUser, new AdminBroadcastRequest("Bảo trì hệ thống", "Từ 23:00 đến 01:00", "ALL"), false
        );
        assertEquals("DRAFT", draft.status());

        AdminBroadcastResponse sent = adminService.sendBroadcast(adminUser, draft.id());
        assertEquals("SENT", sent.status());
        assertEquals(2, sent.recipientCount()); // adminUser + landlordUser only (tenantUser locked)
        assertEquals(2, sentNotifications.size());
    }

    @Test
    void adminChangePassword_updatesTokenVersionAndIssuesFreshJwt() {
        AdminChangePasswordResponse res = adminService.changeAdminPassword(
                adminUser, new ChangePasswordRequest("Admin@123456", "NewAdmin@123456")
        );
        assertNotNull(res.accessToken());
        assertFalse(res.accessToken().isBlank());
        assertEquals(1, userStore.get(1L).getTokenVersion());
        assertEquals(1, jwtUtil.extractTokenVersion(res.accessToken()));
    }

    @Test
    void adminEndpoints_validationRejectsInvalidInputs() throws Exception {
        mockMvc.perform(post("/api/admin/accounts")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AdminCreateAccountRequest("", "bad-email", null, "TENANT", true, null, null, null, null))))
                .andExpect(status().isBadRequest());
    }
}
