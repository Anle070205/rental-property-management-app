package com.roomcare.backend;

import com.roomcare.backend.dto.booking.CreateBookingRequest;
import com.roomcare.backend.dto.booking.RespondBookingRequest;
import com.roomcare.backend.dto.health.HealthFactorResponse;
import com.roomcare.backend.dto.health.RoomHealthScoreResponse;
import com.roomcare.backend.dto.incident.CreateIncidentRequest;
import com.roomcare.backend.dto.room.CreateRoomRequest;
import com.roomcare.backend.dto.room.RoomResponse;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.*;
import com.roomcare.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingAndHealthScoreTest {

    @Mock private RoomRepository roomRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private BookingRequestRepository bookingRequestRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private NotificationService notificationService;
    @Mock private HealthScoreService healthScoreService;

    @InjectMocks private RoomService roomService;
    @InjectMocks private MarketplaceBookingService marketplaceBookingService;
    @InjectMocks private IncidentService incidentService;

    private User landlord;
    private User tenantUser;
    private Property property;
    private Room vacantRoom;

    @BeforeEach
    void setUp() {
        landlord = new User();
        landlord.setId(10L);
        landlord.setFullName("Chủ Nhà Test");
        landlord.setPhone("0981111111");
        landlord.setEmail("landlord.test@roomcare.vn");
        landlord.setRole(Role.LANDLORD);

        tenantUser = new User();
        tenantUser.setId(20L);
        tenantUser.setFullName("Người Thuê Test");
        tenantUser.setPhone("0982222222");
        tenantUser.setEmail("tenant.test@roomcare.vn");
        tenantUser.setRole(Role.TENANT);

        property = new Property();
        property.setId(100L);
        property.setLandlord(landlord);
        property.setName("Nhà trọ Xanh");
        property.setAddress("88 Nguyễn Văn Cừ, Q5");

        vacantRoom = new Room();
        vacantRoom.setId(200L);
        vacantRoom.setProperty(property);
        vacantRoom.setRoomCode("P101");
        vacantRoom.setFloor(1);
        vacantRoom.setArea(new BigDecimal("25"));
        vacantRoom.setRentPrice(new BigDecimal("3500000"));
        vacantRoom.setDeposit(new BigDecimal("3500000"));
        vacantRoom.setCapacity(2);
        vacantRoom.setElectricityPrice(new BigDecimal("3500"));
        vacantRoom.setWaterPrice(new BigDecimal("15000"));
        vacantRoom.setInternetFee(new BigDecimal("100000"));
        vacantRoom.setServiceFee(new BigDecimal("50000"));
        vacantRoom.setAmenities("Điều hòa, Nóng lạnh, Wifi");
        vacantRoom.setStatus(RoomStatus.VACANT);
        vacantRoom.setHealthScore(100);
    }

    private RoomHealthScoreResponse scoreOf(int total) {
        return new RoomHealthScoreResponse(
                200L, "P101", 100L, "Nhà trọ Xanh", RoomStatus.VACANT,
                null, total, "GOOD", "Tốt", "STABLE", 0,
                List.of(new HealthFactorResponse("Tình trạng", 40, 40, 40, "GOOD", "OK")),
                List.of(), List.of(), ""
        );
    }

    @Test
    void createRoom_alwaysDefaultsHealthScoreTo100() {
        when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));
        when(roomRepository.existsByPropertyIdAndRoomCode(100L, "P101")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(200L);
            return r;
        });
        when(healthScoreService.calculateRoomHealthScore(any(Room.class))).thenReturn(scoreOf(100));
        when(bookingRequestRepository.findFirstByRoomIdAndRequestTypeAndStatusOrderByUpdatedAtDesc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(invoiceRepository.findByRoomIdOrderByBillingMonthDesc(200L)).thenReturn(List.of());

        CreateRoomRequest req = new CreateRoomRequest(
                100L, "P101", 1, new BigDecimal("25"),
                new BigDecimal("3500000"), new BigDecimal("3500000"),
                2, "Điều hòa, Nóng lạnh", "Phòng mới", null,
                new BigDecimal("3500"), new BigDecimal("15000"),
                new BigDecimal("100000"), new BigDecimal("50000")
        );

        RoomResponse created = roomService.createRoom(10L, req);
        assertEquals(100, created.getHealthScore());
        assertEquals(RoomStatus.VACANT, created.getStatus());
        assertEquals(new BigDecimal("3500"), created.getElectricityPrice());
    }

    @Test
    void bookingFlow_createsBookingNotifiesAndApprovesRoomAndTenant() {
        when(roomRepository.findById(200L)).thenReturn(Optional.of(vacantRoom));
        when(bookingRequestRepository.existsByTenantUserIdAndRoomIdAndRequestTypeAndStatus(
                20L, 200L, BookingRequest.RequestType.BOOKING, BookingRequest.Status.PENDING))
                .thenReturn(false);
        when(bookingRequestRepository.save(any(BookingRequest.class))).thenAnswer(inv -> {
            BookingRequest b = inv.getArgument(0);
            if (b.getId() == null) b.setId(500L);
            return b;
        });
        when(healthScoreService.calculateRoomHealthScore(vacantRoom)).thenReturn(scoreOf(100));

        CreateBookingRequest createReq = new CreateBookingRequest(
                200L, BookingRequest.RequestType.BOOKING, LocalDate.now().plusDays(5),
                "079200001234", "Em muốn đặt phòng P101 đầu tháng."
        );
        var created = marketplaceBookingService.createRequest(tenantUser, createReq);
        assertEquals(BookingRequest.Status.PENDING, created.getStatus());
        assertEquals(100, created.getRoomHealthScore());
        verify(notificationService, times(2)).createNotification(any(), eq(NotificationType.CONTRACT), anyString(), anyString(), eq(500L));

        BookingRequest pendingEntity = new BookingRequest();
        pendingEntity.setId(500L);
        pendingEntity.setRoom(vacantRoom);
        pendingEntity.setProperty(property);
        pendingEntity.setLandlord(landlord);
        pendingEntity.setTenantUser(tenantUser);
        pendingEntity.setTenantName(tenantUser.getFullName());
        pendingEntity.setTenantPhone(tenantUser.getPhone());
        pendingEntity.setTenantEmail(tenantUser.getEmail());
        pendingEntity.setIdentityNumber("079200001234");
        pendingEntity.setRequestType(BookingRequest.RequestType.BOOKING);
        pendingEntity.setStatus(BookingRequest.Status.PENDING);

        when(bookingRequestRepository.findById(500L)).thenReturn(Optional.of(pendingEntity));
        when(tenantRepository.findByUserId(20L)).thenReturn(Optional.empty());
        when(tenantRepository.findFirstByEmailIgnoreCaseOrPhone(tenantUser.getEmail(), tenantUser.getPhone()))
                .thenReturn(Optional.empty());
        when(tenantRepository.existsByIdentityNumber("079200001234")).thenReturn(false);
        when(bookingRequestRepository.findByRoomIdAndRequestTypeAndStatus(
                200L, BookingRequest.RequestType.BOOKING, BookingRequest.Status.PENDING))
                .thenReturn(List.of(pendingEntity));

        var approved = marketplaceBookingService.respondRequest(
                landlord, 500L, new RespondBookingRequest(BookingRequest.Status.APPROVED, "Chào mừng em nhận phòng"));
        assertEquals(BookingRequest.Status.APPROVED, approved.getStatus());
        assertEquals(RoomStatus.OCCUPIED, vacantRoom.getStatus());
        verify(tenantRepository).save(any(Tenant.class));
        verify(healthScoreService).syncRoomHealthScore(vacantRoom);
    }

    @Test
    void incidentCreate_syncsRoomHealthScoreOnDamageReport() {
        Tenant tenant = new Tenant();
        tenant.setId(300L);
        tenant.setUser(tenantUser);
        tenant.setLandlord(landlord);
        tenant.setProperty(property);
        tenant.setFullName(tenantUser.getFullName());

        BookingRequest approved = new BookingRequest();
        approved.setRoom(vacantRoom);

        when(tenantRepository.findByUserId(20L)).thenReturn(Optional.of(tenant));
        when(bookingRequestRepository.findFirstByTenantUserIdAndRequestTypeAndStatusOrderByUpdatedAtDesc(
                20L, BookingRequest.RequestType.BOOKING, BookingRequest.Status.APPROVED))
                .thenReturn(Optional.of(approved));
        when(incidentsSaveAnswer()).thenAnswer(inv -> inv.getArgument(0));

        incidentService.create(tenantUser, new CreateIncidentRequest(
                Incident.Type.EQUIPMENT, "Hỏng máy lạnh", "Máy lạnh bị chảy nước", Incident.Priority.HIGH, null));

        verify(healthScoreService).syncRoomHealthScore(vacantRoom);
        verify(notificationService).createNotification(eq(landlord), eq(NotificationType.INCIDENT), anyString(), anyString(), any());
    }

    private Incident incidentsSaveAnswer() {
        return incidentRepository.saveAndFlush(any(Incident.class));
    }
}
