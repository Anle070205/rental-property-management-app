package com.roomcare.backend;

import com.roomcare.backend.dto.incident.CreateIncidentRequest;
import com.roomcare.backend.dto.incident.IncidentResponse;
import com.roomcare.backend.dto.incident.UpdateIncidentStatusRequest;
import com.roomcare.backend.dto.room.RoomResponse;
import com.roomcare.backend.dto.roommate.CreateRoommateRequest;
import com.roomcare.backend.dto.roommate.RoommateResponse;
import com.roomcare.backend.dto.tenant.MyRoomResponse;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.*;
import com.roomcare.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RoomOccupancyIncidentMyRoomTest {

    private RoomRepository roomRepository;
    private PropertyRepository propertyRepository;
    private InvoiceRepository invoiceRepository;
    private BookingRequestRepository bookingRequestRepository;
    private HealthScoreService healthScoreService;
    private ContractRepository contractRepository;
    private RoommateRepository roommateRepository;
    private TenantRepository tenantRepository;
    private UserRepository userRepository;
    private PaymentRepository paymentRepository;
    private NotificationService notificationService;
    private IncidentRepository incidentRepository;

    private RoomService roomService;
    private RoommateService roommateService;
    private IncidentService incidentService;
    private TenantPortalProviderImpl tenantPortalProvider;

    private User landlord1;
    private User landlord2;
    private User tenantUser1;
    private Property property1;
    private Room vacantRoom;
    private Room occupiedRoom;
    private Room fullRoom;
    private Tenant primaryTenant;
    private Contract activeContract;
    private final List<Roommate> roommateStore = new ArrayList<>();
    private final Map<Long, Incident> incidentStore = new LinkedHashMap<>();
    private final AtomicLong seq = new AtomicLong(100L);

    @BeforeEach
    void setUp() {
        roommateStore.clear();
        incidentStore.clear();

        roomRepository = mock(RoomRepository.class);
        propertyRepository = mock(PropertyRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        bookingRequestRepository = mock(BookingRequestRepository.class);
        healthScoreService = mock(HealthScoreService.class);
        contractRepository = mock(ContractRepository.class);
        roommateRepository = mock(RoommateRepository.class);
        tenantRepository = mock(TenantRepository.class);
        userRepository = mock(UserRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        notificationService = mock(NotificationService.class);
        incidentRepository = mock(IncidentRepository.class);

        roomService = new RoomService(
                roomRepository,
                propertyRepository,
                invoiceRepository,
                bookingRequestRepository,
                healthScoreService
        );
        ReflectionTestUtils.setField(roomService, "contractRepository", contractRepository);
        ReflectionTestUtils.setField(roomService, "roommateRepository", roommateRepository);

        roommateService = new RoommateService(
                roommateRepository,
                tenantRepository,
                roomRepository,
                contractRepository,
                userRepository
        );

        incidentService = new IncidentService(
                incidentRepository,
                tenantRepository,
                notificationService,
                invoiceRepository,
                bookingRequestRepository,
                roomRepository,
                healthScoreService
        );

        tenantPortalProvider = new TenantPortalProviderImpl(
                invoiceRepository,
                paymentRepository,
                roomRepository,
                bookingRequestRepository,
                notificationService
        );
        ReflectionTestUtils.setField(tenantPortalProvider, "contractRepository", contractRepository);
        ReflectionTestUtils.setField(tenantPortalProvider, "roommateRepository", roommateRepository);

        landlord1 = new User();
        landlord1.setId(1L);
        landlord1.setFullName("Nguyen Van A");
        landlord1.setPhone("0901234567");
        landlord1.setRole(Role.LANDLORD);

        landlord2 = new User();
        landlord2.setId(2L);
        landlord2.setFullName("Tran Thi B");
        landlord2.setRole(Role.LANDLORD);

        property1 = new Property();
        property1.setId(10L);
        property1.setName("Nha tro An Phuc");
        property1.setAddress("123 Nguyen Trai, Q5");
        property1.setLandlord(landlord1);

        vacantRoom = new Room();
        vacantRoom.setId(101L);
        vacantRoom.setProperty(property1);
        vacantRoom.setRoomCode("P.402");
        vacantRoom.setFloor(4);
        vacantRoom.setArea(new BigDecimal("28"));
        vacantRoom.setRentPrice(new BigDecimal("4000000"));
        vacantRoom.setDeposit(new BigDecimal("4000000"));
        vacantRoom.setCapacity(2);
        vacantRoom.setStatus(RoomStatus.VACANT);
        vacantRoom.setHealthScore(100);
        vacantRoom.setCreatedAt(LocalDateTime.now());
        vacantRoom.setUpdatedAt(LocalDateTime.now());

        occupiedRoom = new Room();
        occupiedRoom.setId(102L);
        occupiedRoom.setProperty(property1);
        occupiedRoom.setRoomCode("P.305");
        occupiedRoom.setFloor(3);
        occupiedRoom.setArea(new BigDecimal("24"));
        occupiedRoom.setRentPrice(new BigDecimal("3800000"));
        occupiedRoom.setDeposit(new BigDecimal("7600000"));
        occupiedRoom.setCapacity(2);
        occupiedRoom.setAmenities("Dieu hoa, Nong lanh");
        occupiedRoom.setElectricityPrice(new BigDecimal("3500"));
        occupiedRoom.setWaterPrice(new BigDecimal("15000"));
        occupiedRoom.setInternetFee(new BigDecimal("100000"));
        occupiedRoom.setServiceFee(new BigDecimal("50000"));
        occupiedRoom.setStatus(RoomStatus.OCCUPIED);
        occupiedRoom.setHealthScore(85);
        occupiedRoom.setCreatedAt(LocalDateTime.now());
        occupiedRoom.setUpdatedAt(LocalDateTime.now());

        fullRoom = new Room();
        fullRoom.setId(103L);
        fullRoom.setProperty(property1);
        fullRoom.setRoomCode("P.101");
        fullRoom.setFloor(1);
        fullRoom.setArea(new BigDecimal("18"));
        fullRoom.setRentPrice(new BigDecimal("2500000"));
        fullRoom.setDeposit(new BigDecimal("2500000"));
        fullRoom.setCapacity(1);
        fullRoom.setStatus(RoomStatus.OCCUPIED);
        fullRoom.setHealthScore(90);
        fullRoom.setCreatedAt(LocalDateTime.now());
        fullRoom.setUpdatedAt(LocalDateTime.now());

        tenantUser1 = new User();
        tenantUser1.setId(20L);
        tenantUser1.setFullName("Pham Gia Bao");
        tenantUser1.setEmail("tenant1@example.com");
        tenantUser1.setPhone("0938456210");
        tenantUser1.setRole(Role.TENANT);

        primaryTenant = new Tenant();
        primaryTenant.setId(30L);
        primaryTenant.setUser(tenantUser1);
        primaryTenant.setLandlord(landlord1);
        primaryTenant.setProperty(property1);
        primaryTenant.setFullName("Pham Gia Bao");
        primaryTenant.setPhone("0938456210");

        activeContract = new Contract();
        activeContract.setId(200L);
        activeContract.setContractCode("HD-2026-P305");
        activeContract.setLandlord(landlord1);
        activeContract.setProperty(property1);
        activeContract.setRoom(occupiedRoom);
        activeContract.setTenant(primaryTenant);
        activeContract.setStartDate(LocalDate.of(2026, 1, 1));
        activeContract.setEndDate(LocalDate.of(2026, 12, 31));
        activeContract.setMonthlyRent(new BigDecimal("3800000"));
        activeContract.setDepositAmount(new BigDecimal("7600000"));
        activeContract.setPaymentDayOfMonth(5);
        activeContract.setStatus(ContractStatus.ACTIVE);

        var score = new com.roomcare.backend.dto.health.RoomHealthScoreResponse();
        score.setTotalScore(85);
        when(healthScoreService.calculateRoomHealthScore(any(Room.class))).thenReturn(score);

        when(roomRepository.findById(101L)).thenReturn(Optional.of(vacantRoom));
        when(roomRepository.findById(102L)).thenReturn(Optional.of(occupiedRoom));
        when(roomRepository.findById(103L)).thenReturn(Optional.of(fullRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord1));
        when(tenantRepository.findById(30L)).thenReturn(Optional.of(primaryTenant));
        when(tenantRepository.findByUserId(20L)).thenReturn(Optional.of(primaryTenant));
        when(contractRepository.findById(200L)).thenReturn(Optional.of(activeContract));

        when(contractRepository.findFirstByRoomIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(eq(101L), anyList()))
                .thenReturn(Optional.empty());
        when(contractRepository.findFirstByRoomIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(eq(102L), anyList()))
                .thenReturn(Optional.of(activeContract));
        Contract fullContract = new Contract();
        fullContract.setId(201L);
        fullContract.setContractCode("HD-2026-P101");
        fullContract.setLandlord(landlord1);
        fullContract.setProperty(property1);
        fullContract.setRoom(fullRoom);
        fullContract.setTenant(primaryTenant);
        fullContract.setStatus(ContractStatus.ACTIVE);
        when(contractRepository.findFirstByRoomIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(eq(103L), anyList()))
                .thenReturn(Optional.of(fullContract));

        when(contractRepository.findFirstByTenantIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(eq(30L), anyList()))
                .thenReturn(Optional.of(activeContract));
    }

    @Test
    void roomService_enforcesPrimaryTenantVsRoommateCapacity_andAvoidsDuplicateStatusText() {
        // Vacant room allows primary tenant
        RoomResponse vacantRes = roomService.getRoomById(1L, 101L);
        assertTrue(vacantRes.getCanAddPrimaryTenant());
        assertFalse(vacantRes.getCanAddRoommate());
        assertEquals(0, vacantRes.getCurrentOccupants());

        // Occupied room (capacity 2, 1 primary tenant, 0 roommates) blocks primary tenant, allows roommate
        when(roommateRepository.findByContractIdOrderByCreatedAtAsc(200L)).thenReturn(List.of());
        RoomResponse occRes = roomService.getRoomById(1L, 102L);
        assertFalse(occRes.getCanAddPrimaryTenant());
        assertTrue(occRes.getCanAddRoommate());
        assertEquals(1, occRes.getCurrentOccupants());
        assertEquals("HD-2026-P305", occRes.getCurrentContract()); // No "(ACTIVE) (ACTIVE)"
        assertNotNull(occRes.getOccupancyMessage());

        // Add 1 roommate -> fills capacity (2/2), so full room now blocks roommate too
        Roommate rm = new Roommate();
        rm.setId(50L);
        rm.setLandlord(landlord1);
        rm.setTenant(primaryTenant);
        rm.setRoom(occupiedRoom);
        rm.setContract(activeContract);
        rm.setFullName("Tran Minh Em");
        rm.setRelationship("Em trai");
        when(roommateRepository.findByContractIdOrderByCreatedAtAsc(200L)).thenReturn(List.of(rm));
        RoomResponse nowFullRes = roomService.getRoomById(1L, 102L);
        assertFalse(nowFullRes.getCanAddPrimaryTenant());
        assertFalse(nowFullRes.getCanAddRoommate());
        assertEquals(2, nowFullRes.getCurrentOccupants());
        assertTrue(nowFullRes.getOccupancyMessage().contains("đã đủ số người thuê"));

        // Capacity 1 occupied room blocks both
        when(roommateRepository.findByContractIdOrderByCreatedAtAsc(201L)).thenReturn(List.of());
        RoomResponse cap1Res = roomService.getRoomById(1L, 103L);
        assertFalse(cap1Res.getCanAddPrimaryTenant());
        assertFalse(cap1Res.getCanAddRoommate());
    }

    @Test
    void roommateService_rejectsVacantRoom_andRejectsWhenRoomCapacityFull() {
        CreateRoommateRequest vacantReq = new CreateRoommateRequest();
        vacantReq.setTenantId(30L);
        vacantReq.setRoomId(101L); // Vacant, no active contract
        vacantReq.setFullName("Guest");
        assertThrows(RuntimeException.class, () -> roommateService.addRoommate(1L, vacantReq));

        // Full capacity on P.101 (capacity 1, already has primary tenant)
        when(roommateRepository.countByContractId(201L)).thenReturn(0L);
        CreateRoommateRequest fullReq = new CreateRoommateRequest();
        fullReq.setTenantId(30L);
        fullReq.setRoomId(103L);
        fullReq.setContractId(201L);
        fullReq.setFullName("Extra Person");
        assertThrows(RuntimeException.class, () -> roommateService.addRoommate(1L, fullReq));

        // Capacity 2 on P.305 allows 1 roommate
        when(roommateRepository.countByContractId(200L)).thenReturn(0L);
        when(roommateRepository.save(any(Roommate.class))).thenAnswer(inv -> {
            Roommate r = inv.getArgument(0);
            r.setId(51L);
            return r;
        });
        CreateRoommateRequest okReq = new CreateRoommateRequest();
        okReq.setTenantId(30L);
        okReq.setRoomId(102L);
        okReq.setFullName("Nguoi O Cung Hop Le");
        okReq.setRelationship("Ban");
        RoommateResponse saved = roommateService.addRoommate(1L, okReq);
        assertEquals("Nguoi O Cung Hop Le", saved.getFullName());
    }

    @Test
    void incidentService_landlordListAndUpdateStatus_notifiesTenant_andBlocksCrossLandlord() {
        when(roomRepository.findByPropertyIdAndStatusInOrderByRoomCodeAsc(eq(10L), anyList()))
                .thenReturn(List.of(occupiedRoom));
        when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(inv -> {
            Incident inc = inv.getArgument(0);
            if (inc.getId() == null) {
                inc.setId(seq.getAndIncrement());
                inc.setCreatedAt(LocalDateTime.now());
            }
            inc.setUpdatedAt(LocalDateTime.now());
            incidentStore.put(inc.getId(), inc);
            return inc;
        });

        IncidentResponse created = incidentService.create(tenantUser1, new CreateIncidentRequest(
                Incident.Type.WATER, "Ro ri voi nuoc", "Voi sen bi ri nuoc lien tuc",
                Incident.Priority.HIGH, null
        ));
        assertEquals("P.305", created.roomCode());
        assertEquals("Pham Gia Bao", created.tenantName());
        assertEquals(Incident.Status.RECEIVED, created.status());

        Incident stored = incidentStore.get(created.id());
        when(incidentRepository.findByIdAndTenantLandlordId(created.id(), 1L)).thenReturn(Optional.of(stored));
        when(incidentRepository.findByIdAndTenantLandlordId(created.id(), 2L)).thenReturn(Optional.empty());
        when(incidentRepository.findByTenantLandlordId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stored)));

        // Landlord 2 isolated
        assertThrows(RuntimeException.class, () -> incidentService.getForLandlord(landlord2, created.id()));
        assertThrows(RuntimeException.class, () -> incidentService.updateStatusForLandlord(
                landlord2, created.id(), new UpdateIncidentStatusRequest(Incident.Status.IN_PROGRESS, "Nope")));

        // Landlord 1 lists and resolves
        var listRes = incidentService.listForLandlord(landlord1, null, 0, 20);
        assertEquals(1, listRes.getContent().size());

        IncidentResponse updated = incidentService.updateStatusForLandlord(
                landlord1, created.id(), new UpdateIncidentStatusRequest(Incident.Status.DONE, "Da thay voi moi"));
        assertEquals(Incident.Status.DONE, updated.status());
        assertEquals("Da thay voi moi", updated.landlordResponse());

        verify(notificationService, times(1)).createNotification(
                eq(tenantUser1),
                eq(NotificationType.INCIDENT),
                contains("Cập nhật xử lý sự cố"),
                contains("Đã hoàn thành"),
                eq(created.id())
        );
    }

    @Test
    void tenantPortalMyRoom_returnsSyncedContractRoommatesAndLatestInvoice() {
        Roommate rm = new Roommate();
        rm.setFullName("Tran Minh Em");
        rm.setRelationship("Em trai");
        when(roommateRepository.findByContractIdOrderByCreatedAtAsc(200L)).thenReturn(List.of(rm));

        Invoice inv = new Invoice();
        inv.setBillingMonth("2026-11");
        inv.setTotalAmount(new BigDecimal("4950000"));
        inv.setRemainingAmount(BigDecimal.ZERO);
        inv.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findByTenantIdOrderByBillingMonthDescCreatedAtDesc(30L)).thenReturn(List.of(inv));

        MyRoomResponse res = tenantPortalProvider.room(primaryTenant).orElseThrow();
        assertEquals("P.305", res.roomCode());
        assertEquals(new BigDecimal("3800000"), res.rentPrice());
        assertEquals(new BigDecimal("7600000"), res.deposit());
        assertEquals(2, res.capacity());
        assertEquals(2, res.currentOccupants());
        assertEquals("HD-2026-P305", res.contractCode());
        assertEquals("ACTIVE", res.contractStatus());
        assertEquals(List.of("Tran Minh Em (Em trai)"), res.roommates());
        assertEquals("2026-11", res.latestInvoiceMonth());
        assertEquals("PAID", res.latestInvoiceStatus());
    }
}
