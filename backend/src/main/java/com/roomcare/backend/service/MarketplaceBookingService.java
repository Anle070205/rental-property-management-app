package com.roomcare.backend.service;

import com.roomcare.backend.dto.booking.*;
import com.roomcare.backend.dto.health.RoomHealthScoreResponse;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.BookingRequestRepository;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.RoomRepository;
import com.roomcare.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketplaceBookingService {

    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final TenantRepository tenantRepository;
    private final NotificationService notificationService;
    private final HealthScoreService healthScoreService;

    @Transactional(readOnly = true)
    public List<MarketplacePropertyResponse> browseProperties(String keyword, Boolean onlyVacant) {
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        List<Property> properties = propertyRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        return properties.stream()
                .filter(p -> kw.isEmpty()
                        || containsIgnoreCase(p.getName(), kw)
                        || containsIgnoreCase(p.getAddress(), kw)
                        || containsIgnoreCase(p.getLandlord().getFullName(), kw))
                .map(this::mapProperty)
                .filter(dto -> !Boolean.TRUE.equals(onlyVacant) || dto.getVacantRooms() > 0)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MarketplaceRoomResponse> browsePropertyRooms(
            User tenantUser,
            Long propertyId,
            RoomStatus status,
            String keyword) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà trọ"));

        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        List<Room> rooms = roomRepository.findByPropertyId(
                property.getId(),
                PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "roomCode"))
        ).getContent();

        return rooms.stream()
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> kw.isEmpty()
                        || containsIgnoreCase(r.getRoomCode(), kw)
                        || containsIgnoreCase(r.getAmenities(), kw)
                        || containsIgnoreCase(r.getDescription(), kw))
                .map(r -> mapRoom(tenantUser, r))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MarketplaceRoomResponse getMarketplaceRoomDetail(User tenantUser, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        return mapRoom(tenantUser, room);
    }

    @Transactional
    public BookingRequestResponse createRequest(User tenantUser, CreateBookingRequest req) {
        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        Property property = room.getProperty();
        User landlord = property.getLandlord();

        if (req.getRequestType() == BookingRequest.RequestType.BOOKING) {
            if (room.getStatus() != RoomStatus.VACANT) {
                throw new RuntimeException("Phòng " + room.getRoomCode() + " hiện không còn trống để đặt");
            }
            if (bookingRequestRepository.existsByTenantUserIdAndRoomIdAndRequestTypeAndStatus(
                    tenantUser.getId(), room.getId(), BookingRequest.RequestType.BOOKING, BookingRequest.Status.PENDING)) {
                throw new RuntimeException("Bạn đã gửi yêu cầu đặt phòng " + room.getRoomCode() + " và đang chờ chủ nhà duyệt");
            }
        }

        BookingRequest booking = new BookingRequest();
        booking.setRoom(room);
        booking.setProperty(property);
        booking.setLandlord(landlord);
        booking.setTenantUser(tenantUser);
        booking.setRequestType(req.getRequestType());
        booking.setStatus(BookingRequest.Status.PENDING);
        booking.setTenantName(tenantUser.getFullName());
        booking.setTenantPhone(tenantUser.getPhone());
        booking.setTenantEmail(tenantUser.getEmail());
        booking.setIdentityNumber(req.getIdentityNumber() != null && !req.getIdentityNumber().isBlank()
                ? req.getIdentityNumber().trim() : null);
        booking.setMoveInDate(req.getMoveInDate());
        booking.setMessage(req.getMessage() != null ? req.getMessage().trim() : null);

        BookingRequest saved = bookingRequestRepository.save(booking);

        String actionTitle = req.getRequestType() == BookingRequest.RequestType.BOOKING
                ? "Yêu cầu đặt phòng mới - Phòng " + room.getRoomCode()
                : "Liên hệ từ người thuê - Phòng " + room.getRoomCode();
        String tenantContact = tenantUser.getPrimaryIdentifier();
        String landlordContact = landlord.getPrimaryIdentifier();
        String notePreview = saved.getMessage() != null && !saved.getMessage().isBlank()
                ? " • Lời nhắn: " + saved.getMessage()
                : "";
        notificationService.createNotification(
                landlord,
                NotificationType.CONTRACT,
                actionTitle,
                tenantUser.getFullName() + (!tenantContact.isBlank() ? " (" + tenantContact + ")" : "")
                        + " quan tâm phòng " + room.getRoomCode() + " tại " + property.getName() + notePreview,
                saved.getId()
        );

        String tenantTitle = req.getRequestType() == BookingRequest.RequestType.BOOKING
                ? "Đã gửi yêu cầu đặt phòng " + room.getRoomCode()
                : "Đã gửi liên hệ chủ nhà phòng " + room.getRoomCode();
        notificationService.createNotification(
                tenantUser,
                NotificationType.CONTRACT,
                tenantTitle,
                "Chủ nhà " + landlord.getFullName() + (!landlordContact.isBlank() ? " (" + landlordContact + ")" : "")
                        + " sẽ phản hồi yêu cầu của bạn cho " + property.getName() + ".",
                saved.getId()
        );

        return mapBooking(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingRequestResponse> getTenantRequests(User tenantUser) {
        return bookingRequestRepository.findByTenantUserIdOrderByCreatedAtDesc(tenantUser.getId())
                .stream()
                .map(this::mapBooking)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingRequestResponse cancelTenantRequest(User tenantUser, Long bookingId) {
        BookingRequest booking = bookingRequestRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (!booking.getTenantUser().getId().equals(tenantUser.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy yêu cầu này");
        }
        if (booking.getStatus() != BookingRequest.Status.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy yêu cầu đang chờ duyệt");
        }

        booking.setStatus(BookingRequest.Status.CANCELLED);
        BookingRequest saved = bookingRequestRepository.save(booking);

        notificationService.createNotification(
                booking.getLandlord(),
                NotificationType.CONTRACT,
                "Người thuê đã hủy yêu cầu - Phòng " + booking.getRoom().getRoomCode(),
                tenantUser.getFullName() + " đã hủy yêu cầu cho phòng " + booking.getRoom().getRoomCode() + ".",
                saved.getId()
        );
        return mapBooking(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingRequestResponse> getLandlordRequests(
            User landlord,
            BookingRequest.Status status,
            BookingRequest.RequestType type) {
        return bookingRequestRepository.findByLandlordIdOrderByCreatedAtDesc(landlord.getId())
                .stream()
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> type == null || b.getRequestType() == type)
                .map(this::mapBooking)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingRequestResponse respondRequest(User landlord, Long bookingId, RespondBookingRequest req) {
        BookingRequest booking = bookingRequestRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (!booking.getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền xử lý yêu cầu này");
        }
        if (booking.getStatus() != BookingRequest.Status.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý trước đó");
        }
        if (req.getStatus() != BookingRequest.Status.APPROVED
                && req.getStatus() != BookingRequest.Status.REJECTED) {
            throw new RuntimeException("Trạng thái phản hồi chỉ hỗ trợ APPROVED hoặc REJECTED");
        }

        Room room = booking.getRoom();
        String note = req.getLandlordNote() != null ? req.getLandlordNote().trim() : null;

        if (req.getStatus() == BookingRequest.Status.APPROVED
                && booking.getRequestType() == BookingRequest.RequestType.BOOKING) {
            if (room.getStatus() != RoomStatus.VACANT) {
                throw new RuntimeException("Phòng " + room.getRoomCode() + " không còn trống để duyệt đặt phòng");
            }
            room.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room);
            healthScoreService.syncRoomHealthScore(room);

            linkOrCreateTenant(booking, landlord, room.getProperty());

            // Auto-reject any competing pending booking requests for the same room
            List<BookingRequest> competing = bookingRequestRepository.findByRoomIdAndRequestTypeAndStatus(
                    room.getId(), BookingRequest.RequestType.BOOKING, BookingRequest.Status.PENDING);
            for (BookingRequest other : competing) {
                if (!other.getId().equals(booking.getId())) {
                    other.setStatus(BookingRequest.Status.REJECTED);
                    other.setLandlordNote("Phòng đã được duyệt cho người thuê khác.");
                    bookingRequestRepository.save(other);
                    notificationService.createNotification(
                            other.getTenantUser(),
                            NotificationType.CONTRACT,
                            "Yêu cầu đặt phòng " + room.getRoomCode() + " không được duyệt",
                            "Phòng " + room.getRoomCode() + " tại " + booking.getProperty().getName()
                                    + " đã được cho thuê.",
                            other.getId()
                    );
                }
            }
        }

        booking.setStatus(req.getStatus());
        booking.setLandlordNote(note);
        BookingRequest saved = bookingRequestRepository.save(booking);

        String tenantTitle;
        if (booking.getRequestType() == BookingRequest.RequestType.BOOKING) {
            tenantTitle = req.getStatus() == BookingRequest.Status.APPROVED
                    ? "Đặt phòng " + room.getRoomCode() + " đã được duyệt!"
                    : "Yêu cầu đặt phòng " + room.getRoomCode() + " bị từ chối";
        } else {
            tenantTitle = req.getStatus() == BookingRequest.Status.APPROVED
                    ? "Chủ nhà đã phản hồi liên hệ - Phòng " + room.getRoomCode()
                    : "Liên hệ phòng " + room.getRoomCode() + " đã đóng";
        }
        String tenantContent = "Phòng " + room.getRoomCode() + " (" + booking.getProperty().getName() + ")"
                + (note != null && !note.isBlank() ? " • Ghi chú chủ nhà: " + note : "");
        notificationService.createNotification(
                booking.getTenantUser(),
                NotificationType.CONTRACT,
                tenantTitle,
                tenantContent,
                saved.getId()
        );

        return mapBooking(saved);
    }

    private void linkOrCreateTenant(BookingRequest booking, User landlord, Property property) {
        User tenantUser = booking.getTenantUser();
        Optional<Tenant> existingOpt = tenantRepository.findByUserId(tenantUser.getId())
                .or(() -> tenantRepository.findFirstByEmailIgnoreCaseOrPhone(tenantUser.getEmail(), tenantUser.getPhone()))
                .or(() -> tenantUser.getPhone() != null && !tenantUser.getPhone().isBlank()
                        ? tenantRepository.findFirstByPhoneIn(
                                com.roomcare.backend.util.PhoneNumberUtils.lookupVariants(tenantUser.getPhone()))
                        : Optional.empty());

        Tenant tenant = existingOpt.orElseGet(Tenant::new);
        tenant.setUser(tenantUser);
        tenant.setLandlord(landlord);
        tenant.setProperty(property);
        tenant.setFullName(tenantUser.getFullName());
        tenant.setPhone(tenantUser.getPhone());
        tenant.setEmail(tenantUser.getEmail());

        if (tenant.getId() == null) {
            tenant.setIdentityNumber(resolveUniqueIdentityNumber(booking.getIdentityNumber(), tenantUser.getId()));
        } else if (booking.getIdentityNumber() != null
                && !booking.getIdentityNumber().isBlank()
                && !booking.getIdentityNumber().equals(tenant.getIdentityNumber())
                && !tenantRepository.existsByIdentityNumber(booking.getIdentityNumber())) {
            tenant.setIdentityNumber(booking.getIdentityNumber());
        }

        tenantRepository.save(tenant);
    }

    private String resolveUniqueIdentityNumber(String requestedId, Long userId) {
        if (requestedId != null && !requestedId.isBlank() && !tenantRepository.existsByIdentityNumber(requestedId.trim())) {
            return requestedId.trim();
        }
        String candidate = String.format("RC%010d", userId != null ? userId : System.currentTimeMillis() % 1000000000L);
        int suffix = 1;
        while (tenantRepository.existsByIdentityNumber(candidate)) {
            candidate = String.format("RC%08d%02d", userId != null ? userId : 0L, suffix++);
        }
        return candidate;
    }

    private MarketplacePropertyResponse mapProperty(Property p) {
        List<Room> rooms = roomRepository.findByPropertyId(p.getId(), PageRequest.of(0, 500)).getContent();
        int totalRooms = rooms.size();
        int vacantRooms = (int) rooms.stream().filter(r -> r.getStatus() == RoomStatus.VACANT).count();
        BigDecimal minPrice = rooms.stream()
                .map(Room::getRentPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        BigDecimal maxPrice = rooms.stream()
                .map(Room::getRentPrice)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        int avgScore = rooms.isEmpty()
                ? 100
                : (int) Math.round(rooms.stream()
                        .mapToInt(r -> healthScoreService.calculateRoomHealthScore(r).getTotalScore())
                        .average()
                        .orElse(100));

        return new MarketplacePropertyResponse(
                p.getId(),
                p.getName(),
                p.getAddress(),
                p.getDescription(),
                p.getLandlord().getId(),
                p.getLandlord().getFullName(),
                p.getLandlord().getPhone(),
                p.getLandlord().getEmail(),
                totalRooms,
                vacantRooms,
                minPrice,
                maxPrice,
                avgScore
        );
    }

    private MarketplaceRoomResponse mapRoom(User tenantUser, Room room) {
        Property p = room.getProperty();
        User landlord = p.getLandlord();
        RoomHealthScoreResponse hs = healthScoreService.calculateRoomHealthScore(room);
        boolean hasPending = tenantUser != null
                && bookingRequestRepository.existsByTenantUserIdAndRoomIdAndRequestTypeAndStatus(
                tenantUser.getId(), room.getId(), BookingRequest.RequestType.BOOKING, BookingRequest.Status.PENDING);

        return new MarketplaceRoomResponse(
                room.getId(),
                p.getId(),
                p.getName(),
                p.getAddress(),
                landlord.getId(),
                landlord.getFullName(),
                landlord.getPhone(),
                landlord.getEmail(),
                room.getRoomCode(),
                room.getFloor(),
                room.getArea(),
                room.getRentPrice(),
                room.getDeposit(),
                room.getCapacity(),
                room.getElectricityPrice(),
                room.getWaterPrice(),
                room.getInternetFee(),
                room.getServiceFee(),
                room.getAmenities(),
                room.getDescription(),
                room.getImageUrl(),
                room.getStatus(),
                hs.getTotalScore(),
                hs.getRatingLabel(),
                hasPending
        );
    }

    private BookingRequestResponse mapBooking(BookingRequest b) {
        Room r = b.getRoom();
        Property p = b.getProperty();
        User l = b.getLandlord();
        int score = healthScoreService.calculateRoomHealthScore(r).getTotalScore();
        return new BookingRequestResponse(
                b.getId(),
                r.getId(),
                r.getRoomCode(),
                p.getId(),
                p.getName(),
                p.getAddress(),
                r.getRentPrice(),
                r.getDeposit(),
                score,
                l.getId(),
                l.getFullName(),
                l.getPhone(),
                b.getTenantUser().getId(),
                b.getTenantName(),
                b.getTenantPhone(),
                b.getTenantEmail(),
                b.getIdentityNumber(),
                b.getRequestType(),
                b.getStatus(),
                b.getMoveInDate(),
                b.getMessage(),
                b.getLandlordNote(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }

    private boolean containsIgnoreCase(String value, String kwLower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(kwLower);
    }
}
