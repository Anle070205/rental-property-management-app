package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.gueststay.CreateGuestStayRequest;
import com.roomcare.backend.dto.gueststay.GuestStayResponse;
import com.roomcare.backend.dto.gueststay.UpdateGuestStayStatusRequest;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.*;
import com.roomcare.backend.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuestStayService {

    private static final int DATA_RETENTION_DAYS = 30;

    private final GuestStayRepository guestStays;
    private final TenantRepository tenants;
    private final NotificationService notifications;
    private final InvoiceRepository invoiceRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public PageResponse<GuestStayResponse> listForTenant(
            User user,
            GuestStay.Status status,
            int page,
            int size
    ) {
        validatePagination(page, size);
        Optional<Tenant> tenantOpt = optionalTenantFor(user);
        if (tenantOpt.isEmpty()) {
            return PageResponse.of(List.of(), page, size, 0, 0);
        }
        Long tenantId = tenantOpt.get().getId();
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = status == null
                ? guestStays.findByTenantId(tenantId, pageable)
                : guestStays.findByTenantIdAndStatus(tenantId, status, pageable);
        return PageResponse.of(
                result.map(GuestStayResponse::from).getContent(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public GuestStayResponse getForTenant(User user, Long id) {
        Long tenantId = tenantFor(user).getId();
        GuestStay entity = guestStays.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khai báo khách lưu trú"));
        return GuestStayResponse.from(entity);
    }

    @Transactional
    public GuestStayResponse create(User user, CreateGuestStayRequest request) {
        Tenant tenant = optionalTenantFor(user)
                .orElseThrow(() -> new RuntimeException("Bạn cần đang thuê phòng để khai báo khách lưu trú"));
        Room resolvedRoom = resolveTenantRoom(user, tenant);
        if (resolvedRoom == null) {
            throw new RuntimeException("Chưa xác định được phòng đang thuê của bạn");
        }
        if (!request.stayEndAt().isAfter(request.stayStartAt())) {
            throw new RuntimeException("Thời gian kết thúc dự kiến phải sau thời gian bắt đầu lưu trú");
        }

        String normalizedId = request.guestIdentityNumber().trim();
        GuestStay stay = new GuestStay();
        stay.setTenant(tenant);
        stay.setRoom(resolvedRoom);
        stay.setLandlord(tenant.getLandlord());
        stay.setGuestFullName(request.guestFullName().trim());
        stay.setGuestIdentityNumber(normalizedId);
        stay.setGuestDateOfBirth(request.guestDateOfBirth());
        stay.setGuestGender(trimToNull(request.guestGender()));
        stay.setGuestPhone(trimToNull(request.guestPhone()));
        stay.setRelationship(request.relationship());
        stay.setStayStartAt(request.stayStartAt());
        stay.setStayEndAt(request.stayEndAt());
        stay.setNote(trimToNull(request.note()));
        stay.setConsentConfirmed(true);
        stay.setQrVerified(request.qrVerified());
        stay.setStatus(GuestStay.Status.PENDING);
        stay.setRetentionUntil(request.stayEndAt().plusDays(DATA_RETENTION_DAYS));

        GuestStay saved = guestStays.saveAndFlush(stay);

        notifications.createNotification(
                tenant.getLandlord(),
                NotificationType.SYSTEM,
                "Khai báo khách lưu trú mới",
                tenant.getFullName()
                        + " (Phòng " + resolvedRoom.getRoomCode() + ") đã khai báo khách "
                        + saved.getGuestFullName()
                        + " (" + relationshipLabel(saved.getRelationship()) + ").",
                saved.getId()
        );

        return GuestStayResponse.from(saved);
    }

    @Transactional
    public GuestStayResponse cancelForTenant(User user, Long id) {
        Tenant tenant = tenantFor(user);
        GuestStay stay = guestStays.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khai báo khách lưu trú"));
        if (stay.getStatus() != GuestStay.Status.PENDING && stay.getStatus() != GuestStay.Status.NEEDS_INFO) {
            throw new RuntimeException("Chỉ có thể hủy khai báo đang chờ xử lý hoặc cần bổ sung thông tin");
        }
        stay.setStatus(GuestStay.Status.CANCELLED);
        guestStays.saveAndFlush(stay);

        notifications.createNotification(
                stay.getLandlord(),
                NotificationType.SYSTEM,
                "Khách thuê đã hủy khai báo lưu trú",
                tenant.getFullName()
                        + (stay.getRoom() != null ? " (Phòng " + stay.getRoom().getRoomCode() + ")" : "")
                        + " đã hủy khai báo khách " + stay.getGuestFullName() + ".",
                stay.getId()
        );

        return GuestStayResponse.from(stay);
    }

    @Transactional
    public void deleteForTenant(User user, Long id) {
        Tenant tenant = tenantFor(user);
        GuestStay stay = guestStays.findByIdAndTenantId(id, tenant.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khai báo khách lưu trú"));
        if (stay.getStatus() != GuestStay.Status.CANCELLED && stay.getStatus() != GuestStay.Status.COMPLETED) {
            throw new RuntimeException("Chỉ có thể xóa khai báo đã hoàn tất hoặc đã hủy");
        }
        guestStays.delete(stay);
    }

    @Transactional(readOnly = true)
    public PageResponse<GuestStayResponse> listForLandlord(
            User user,
            GuestStay.Status status,
            int page,
            int size
    ) {
        validatePagination(page, size);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = status == null
                ? guestStays.findByLandlordId(user.getId(), pageable)
                : guestStays.findByLandlordIdAndStatus(user.getId(), status, pageable);
        return PageResponse.of(
                result.map(GuestStayResponse::from).getContent(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public GuestStayResponse getForLandlord(User user, Long id) {
        GuestStay entity = guestStays.findByIdAndLandlordId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khai báo khách lưu trú"));
        return GuestStayResponse.from(entity);
    }

    @Transactional
    public GuestStayResponse updateStatus(User user, Long id, UpdateGuestStayStatusRequest request) {
        GuestStay stay = guestStays.findByIdAndLandlordId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khai báo khách lưu trú"));

        if (stay.getStatus() == GuestStay.Status.CANCELLED) {
            throw new RuntimeException("Khai báo đã bị hủy, không thể cập nhật trạng thái");
        }
        if (request.status() == GuestStay.Status.CANCELLED) {
            throw new RuntimeException("Chủ nhà trọ không sử dụng trạng thái hủy; hãy chọn Đã tiếp nhận, Cần bổ sung hoặc Hoàn tất");
        }

        stay.setStatus(request.status());
        if (request.landlordNote() != null) {
            stay.setLandlordNote(trimToNull(request.landlordNote()));
        }
        GuestStay saved = guestStays.saveAndFlush(stay);

        User tenantUser = stay.getTenant() != null ? stay.getTenant().getUser() : null;
        if (tenantUser != null) {
            notifications.createNotification(
                    tenantUser,
                    NotificationType.SYSTEM,
                    "Cập nhật khai báo khách lưu trú",
                    "Khai báo khách " + stay.getGuestFullName()
                            + " đã chuyển sang trạng thái: " + statusLabel(saved.getStatus())
                            + (saved.getLandlordNote() != null ? " • " + saved.getLandlordNote() : ""),
                    saved.getId()
            );
        }

        return GuestStayResponse.from(saved);
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new RuntimeException("Trang phải từ 0 và kích thước từ 1 đến 100");
        }
    }

    private Room resolveTenantRoom(User user, Tenant tenant) {
        if (bookingRequestRepository != null && user != null) {
            Optional<BookingRequest> approved = bookingRequestRepository
                    .findFirstByTenantUserIdAndRequestTypeAndStatusOrderByUpdatedAtDesc(
                            user.getId(), BookingRequest.RequestType.BOOKING, BookingRequest.Status.APPROVED);
            if (approved.isPresent()) {
                return approved.get().getRoom();
            }
        }
        if (invoiceRepository != null) {
            Optional<Room> fromInvoice = invoiceRepository
                    .findByTenantIdOrderByBillingMonthDescCreatedAtDesc(tenant.getId())
                    .stream()
                    .findFirst()
                    .map(Invoice::getRoom);
            if (fromInvoice.isPresent()) {
                return fromInvoice.get();
            }
        }
        if (roomRepository != null && tenant.getProperty() != null) {
            List<Room> occupiedRooms = roomRepository.findByPropertyIdAndStatusInOrderByRoomCodeAsc(
                    tenant.getProperty().getId(),
                    List.of(RoomStatus.OCCUPIED, RoomStatus.EXPIRING_SOON, RoomStatus.NEEDS_ATTENTION)
            );
            if (!occupiedRooms.isEmpty()) {
                return occupiedRooms.get(0);
            }
        }
        return null;
    }

    private Optional<Tenant> optionalTenantFor(User user) {
        return tenants.findByUserId(user.getId())
                .or(() -> tenants.findFirstByEmailIgnoreCaseOrPhone(user.getEmail(), user.getPhone()))
                .or(() -> user.getPhone() != null && !user.getPhone().isBlank()
                        ? tenants.findFirstByPhoneIn(PhoneNumberUtils.lookupVariants(user.getPhone()))
                        : Optional.empty());
    }

    private Tenant tenantFor(User user) {
        return optionalTenantFor(user)
                .orElseThrow(() -> new RuntimeException("Tài khoản chưa được liên kết với người thuê"));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String relationshipLabel(GuestStay.Relationship rel) {
        return switch (rel) {
            case FRIEND -> "Bạn bè";
            case RELATIVE -> "Người thân";
            case OTHER -> "Khác";
        };
    }

    private String statusLabel(GuestStay.Status status) {
        return switch (status) {
            case PENDING -> "Chờ xử lý";
            case RECEIVED -> "Đã tiếp nhận";
            case NEEDS_INFO -> "Cần bổ sung thông tin";
            case COMPLETED -> "Hoàn tất";
            case CANCELLED -> "Đã hủy";
        };
    }
}
