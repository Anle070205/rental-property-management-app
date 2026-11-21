package com.roomcare.backend;

import com.roomcare.backend.dto.gueststay.CreateGuestStayRequest;
import com.roomcare.backend.dto.gueststay.GuestStayResponse;
import com.roomcare.backend.dto.gueststay.UpdateGuestStayStatusRequest;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.*;
import com.roomcare.backend.service.GuestStayService;
import com.roomcare.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GuestStayTest {

    private GuestStayRepository guestStayRepository;
    private TenantRepository tenantRepository;
    private NotificationService notificationService;
    private InvoiceRepository invoiceRepository;
    private BookingRequestRepository bookingRequestRepository;
    private RoomRepository roomRepository;
    private GuestStayService guestStayService;

    private final Map<Long, GuestStay> stayStore = new LinkedHashMap<>();
    private final AtomicLong stayIdSeq = new AtomicLong(1L);

    private User landlordUser;
    private User otherLandlordUser;
    private User tenantUser1;
    private User tenantUser2;
    private Tenant tenant1;
    private Tenant tenant2;
    private Property property1;
    private Room room305;

    @BeforeEach
    void setUp() {
        stayStore.clear();
        stayIdSeq.set(1L);

        guestStayRepository = mock(GuestStayRepository.class);
        tenantRepository = mock(TenantRepository.class);
        notificationService = mock(NotificationService.class);
        invoiceRepository = mock(InvoiceRepository.class);
        bookingRequestRepository = mock(BookingRequestRepository.class);
        roomRepository = mock(RoomRepository.class);

        guestStayService = new GuestStayService(
                guestStayRepository,
                tenantRepository,
                notificationService,
                invoiceRepository,
                bookingRequestRepository,
                roomRepository
        );

        landlordUser = new User();
        landlordUser.setId(1L);
        landlordUser.setFullName("Chu Tro A");
        landlordUser.setRole(Role.LANDLORD);

        otherLandlordUser = new User();
        otherLandlordUser.setId(2L);
        otherLandlordUser.setFullName("Chu Tro B");
        otherLandlordUser.setRole(Role.LANDLORD);

        property1 = new Property();
        property1.setId(10L);
        property1.setName("Nha tro An Phuc");
        property1.setLandlord(landlordUser);

        room305 = new Room();
        room305.setId(100L);
        room305.setRoomCode("P.305");
        room305.setProperty(property1);
        room305.setStatus(RoomStatus.OCCUPIED);

        tenantUser1 = new User();
        tenantUser1.setId(11L);
        tenantUser1.setFullName("Nguyen Van Thue");
        tenantUser1.setEmail("tenant1@gmail.com");
        tenantUser1.setPhone("0901111111");
        tenantUser1.setRole(Role.TENANT);

        tenantUser2 = new User();
        tenantUser2.setId(12L);
        tenantUser2.setFullName("Tran Thi Thue 2");
        tenantUser2.setEmail("tenant2@gmail.com");
        tenantUser2.setPhone("0902222222");
        tenantUser2.setRole(Role.TENANT);

        tenant1 = new Tenant();
        tenant1.setId(21L);
        tenant1.setUser(tenantUser1);
        tenant1.setLandlord(landlordUser);
        tenant1.setProperty(property1);
        tenant1.setFullName("Nguyen Van Thue");

        tenant2 = new Tenant();
        tenant2.setId(22L);
        tenant2.setUser(tenantUser2);
        tenant2.setLandlord(landlordUser);
        tenant2.setProperty(property1);
        tenant2.setFullName("Tran Thi Thue 2");

        when(tenantRepository.findByUserId(11L)).thenReturn(Optional.of(tenant1));
        when(tenantRepository.findByUserId(12L)).thenReturn(Optional.of(tenant2));
        when(roomRepository.findByPropertyIdAndStatusInOrderByRoomCodeAsc(eq(10L), anyList()))
                .thenReturn(List.of(room305));

        when(guestStayRepository.saveAndFlush(any(GuestStay.class))).thenAnswer(inv -> {
            GuestStay g = inv.getArgument(0);
            if (g.getId() == null) {
                g.setId(stayIdSeq.getAndIncrement());
                g.setCreatedAt(LocalDateTime.now());
            }
            g.setUpdatedAt(LocalDateTime.now());
            stayStore.put(g.getId(), g);
            return g;
        });

        when(guestStayRepository.findByIdAndTenantId(anyLong(), anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            Long tId = inv.getArgument(1);
            GuestStay g = stayStore.get(id);
            if (g != null && g.getTenant() != null && tId.equals(g.getTenant().getId())) {
                return Optional.of(g);
            }
            return Optional.empty();
        });

        when(guestStayRepository.findByIdAndLandlordId(anyLong(), anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            Long lId = inv.getArgument(1);
            GuestStay g = stayStore.get(id);
            if (g != null && g.getLandlord() != null && lId.equals(g.getLandlord().getId())) {
                return Optional.of(g);
            }
            return Optional.empty();
        });

        when(guestStayRepository.findByTenantId(anyLong(), any(Pageable.class))).thenAnswer(inv -> {
            Long tId = inv.getArgument(0);
            List<GuestStay> items = stayStore.values().stream()
                    .filter(g -> g.getTenant() != null && tId.equals(g.getTenant().getId()))
                    .toList();
            return new PageImpl<>(items);
        });

        when(guestStayRepository.findByLandlordId(anyLong(), any(Pageable.class))).thenAnswer(inv -> {
            Long lId = inv.getArgument(0);
            List<GuestStay> items = stayStore.values().stream()
                    .filter(g -> g.getLandlord() != null && lId.equals(g.getLandlord().getId()))
                    .toList();
            return new PageImpl<>(items);
        });
    }

    @Test
    void guestStayLifecycle_masksIdentity_notifiesLandlordAndTenant_andEnforcesIsolation() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 21, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 22, 10, 0);

        // 1. Tenant 1 creates guest stay for a friend (via CCCD QR)
        CreateGuestStayRequest req = new CreateGuestStayRequest(
                "Le Minh Khach",
                "079200001234",
                LocalDate.of(1999, 5, 10),
                "Nam",
                "0912345678",
                GuestStay.Relationship.FRIEND,
                start,
                end,
                "Ban den choi qua dem",
                true,
                true
        );

        GuestStayResponse created = guestStayService.create(tenantUser1, req);
        assertNotNull(created.id());
        assertEquals("079*****1234", created.maskedIdentityNumber());
        assertEquals(GuestStay.Status.PENDING, created.status());
        assertTrue(created.qrVerified());
        assertEquals("P.305", created.roomCode());
        assertNotNull(created.legalDisclaimer());

        verify(notificationService, times(1)).createNotification(
                eq(landlordUser),
                eq(NotificationType.SYSTEM),
                contains("Khai báo khách lưu trú"),
                contains("Le Minh Khach"),
                eq(created.id())
        );

        // 2. Tenant 2 cannot access Tenant 1's guest stay
        assertThrows(RuntimeException.class, () -> guestStayService.getForTenant(tenantUser2, created.id()));
        assertThrows(RuntimeException.class, () -> guestStayService.cancelForTenant(tenantUser2, created.id()));

        // 3. Other landlord cannot access or update Landlord 1's guest stay
        assertThrows(RuntimeException.class, () -> guestStayService.getForLandlord(otherLandlordUser, created.id()));
        assertThrows(RuntimeException.class, () -> guestStayService.updateStatus(
                otherLandlordUser,
                created.id(),
                new UpdateGuestStayStatusRequest(GuestStay.Status.RECEIVED, "OK")
        ));

        // 4. Landlord 1 updates status to RECEIVED -> notifies Tenant 1
        GuestStayResponse updated = guestStayService.updateStatus(
                landlordUser,
                created.id(),
                new UpdateGuestStayStatusRequest(GuestStay.Status.RECEIVED, "Da ghi nhan khach")
        );
        assertEquals(GuestStay.Status.RECEIVED, updated.status());
        assertEquals("Da ghi nhan khach", updated.landlordNote());
        verify(notificationService, times(1)).createNotification(
                eq(tenantUser1),
                eq(NotificationType.SYSTEM),
                contains("Cập nhật khai báo"),
                contains("Đã tiếp nhận"),
                eq(created.id())
        );

        // 5. Tenant 1 creates a second stay (relative) and cancels while PENDING
        CreateGuestStayRequest req2 = new CreateGuestStayRequest(
                "Nguyen Thi Than",
                "079200009999",
                LocalDate.of(1975, 1, 1),
                "Nu",
                null,
                GuestStay.Relationship.RELATIVE,
                start,
                end,
                null,
                true,
                false
        );
        GuestStayResponse second = guestStayService.create(tenantUser1, req2);
        GuestStayResponse cancelled = guestStayService.cancelForTenant(tenantUser1, second.id());
        assertEquals(GuestStay.Status.CANCELLED, cancelled.status());

        // 6. Invalid end <= start rejected
        CreateGuestStayRequest invalidTime = new CreateGuestStayRequest(
                "Invalid Time",
                "079200001111",
                null,
                null,
                null,
                GuestStay.Relationship.OTHER,
                end,
                start,
                null,
                true,
                false
        );
        assertThrows(RuntimeException.class, () -> guestStayService.create(tenantUser1, invalidTime));
    }
}
