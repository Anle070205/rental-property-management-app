package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.room.CreateRoomRequest;
import com.roomcare.backend.dto.room.RoomResponse;
import com.roomcare.backend.dto.room.UpdateRoomRequest;
import com.roomcare.backend.entity.BookingRequest;
import com.roomcare.backend.entity.Invoice;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.Room;
import com.roomcare.backend.entity.RoomStatus;
import com.roomcare.backend.repository.BookingRequestRepository;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final PropertyRepository propertyRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final HealthScoreService healthScoreService;

    @Transactional
    public RoomResponse createRoom(Long landlordId, CreateRoomRequest request) {
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà trọ"));

        if (!property.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền thêm phòng vào nhà trọ này");
        }

        String normalizedCode = request.getRoomCode().trim();
        if (roomRepository.existsByPropertyIdAndRoomCode(request.getPropertyId(), normalizedCode)) {
            throw new RuntimeException("Mã phòng đã tồn tại trong nhà trọ này");
        }

        if (request.getRentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá thuê phải lớn hơn 0");
        }

        if (request.getDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Tiền đặt cọc phải lớn hơn hoặc bằng 0");
        }

        validateNonNegative(request.getElectricityPrice(), "Đơn giá điện");
        validateNonNegative(request.getWaterPrice(), "Đơn giá nước");
        validateNonNegative(request.getInternetFee(), "Phí internet");
        validateNonNegative(request.getServiceFee(), "Phí dịch vụ");

        Room room = new Room();
        room.setProperty(property);
        room.setRoomCode(normalizedCode);
        room.setFloor(request.getFloor());
        room.setArea(request.getArea());
        room.setRentPrice(request.getRentPrice());
        room.setDeposit(request.getDeposit());
        room.setCapacity(request.getCapacity());
        room.setElectricityPrice(request.getElectricityPrice());
        room.setWaterPrice(request.getWaterPrice());
        room.setInternetFee(request.getInternetFee());
        room.setServiceFee(request.getServiceFee());
        room.setAmenities(request.getAmenities());
        room.setDescription(request.getDescription());
        room.setImageUrl(request.getImageUrl());
        room.setStatus(RoomStatus.VACANT);
        // Mọi phòng khi mới tạo luôn mặc định Health Score = 100 để làm cơ sở đối chiếu hư hỏng
        room.setHealthScore(100);

        Room saved = roomRepository.save(room);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getRoomsByProperty(
            Long landlordId,
            Long propertyId,
            RoomStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer floor,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        if (!propertyRepository.existsByIdAndLandlordId(propertyId, landlordId)) {
            throw new RuntimeException("Bạn không có quyền xem phòng của nhà trọ này");
        }

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Room> roomPage = roomRepository.findByPropertyIdWithFilters(
                propertyId, status, minPrice, maxPrice, floor, pageable
        );

        List<RoomResponse> content = roomPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(
                content,
                roomPage.getNumber(),
                roomPage.getSize(),
                roomPage.getTotalElements(),
                roomPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getAllRoomsByLandlord(
            Long landlordId,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Room> roomPage = roomRepository.findByLandlordId(landlordId, pageable);

        List<RoomResponse> content = roomPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.of(
                content,
                roomPage.getNumber(),
                roomPage.getSize(),
                roomPage.getTotalElements(),
                roomPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long landlordId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền xem phòng này");
        }

        return mapToResponse(room);
    }

    @Transactional
    public RoomResponse updateRoom(Long landlordId, Long roomId, UpdateRoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật phòng này");
        }

        String normalizedCode = request.getRoomCode().trim();
        if (!room.getRoomCode().equals(normalizedCode)) {
            if (roomRepository.existsByPropertyIdAndRoomCode(room.getProperty().getId(), normalizedCode)) {
                throw new RuntimeException("Mã phòng đã tồn tại trong nhà trọ này");
            }
        }

        if (request.getRentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá thuê phải lớn hơn 0");
        }

        if (request.getDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Tiền đặt cọc phải lớn hơn hoặc bằng 0");
        }

        validateNonNegative(request.getElectricityPrice(), "Đơn giá điện");
        validateNonNegative(request.getWaterPrice(), "Đơn giá nước");
        validateNonNegative(request.getInternetFee(), "Phí internet");
        validateNonNegative(request.getServiceFee(), "Phí dịch vụ");

        room.setRoomCode(normalizedCode);
        room.setFloor(request.getFloor());
        room.setArea(request.getArea());
        room.setRentPrice(request.getRentPrice());
        room.setDeposit(request.getDeposit());
        room.setCapacity(request.getCapacity());
        room.setElectricityPrice(request.getElectricityPrice());
        room.setWaterPrice(request.getWaterPrice());
        room.setInternetFee(request.getInternetFee());
        room.setServiceFee(request.getServiceFee());
        room.setAmenities(request.getAmenities());
        room.setDescription(request.getDescription());
        room.setImageUrl(request.getImageUrl());
        room.setStatus(request.getStatus());
        if (room.getHealthScore() == null) {
            room.setHealthScore(100);
        }

        Room updated = roomRepository.save(room);
        if (healthScoreService != null) {
            healthScoreService.syncRoomHealthScore(updated);
        }
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteRoom(Long landlordId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền xóa phòng này");
        }

        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new RuntimeException("Không thể xóa phòng đang có người thuê");
        }

        List<Invoice> roomInvoices = invoiceRepository.findByRoomIdOrderByBillingMonthDesc(roomId);
        boolean hasUnpaid = roomInvoices.stream()
                .anyMatch(inv -> inv.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0);
        if (hasUnpaid) {
            throw new RuntimeException("Không thể xóa phòng đang còn hóa đơn chưa thanh toán");
        }

        if (!roomInvoices.isEmpty()) {
            throw new RuntimeException("Phòng đã phát sinh hóa đơn lịch sử, không thể xóa trực tiếp");
        }

        roomRepository.delete(room);
    }

    private void validateNonNegative(BigDecimal value, String label) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(label + " phải lớn hơn hoặc bằng 0");
        }
    }

    private RoomResponse mapToResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setPropertyId(room.getProperty().getId());
        response.setPropertyName(room.getProperty().getName());
        response.setPropertyAddress(room.getProperty().getAddress());
        response.setLandlordName(room.getProperty().getLandlord().getFullName());
        response.setLandlordPhone(room.getProperty().getLandlord().getPhone());
        response.setRoomCode(room.getRoomCode());
        response.setFloor(room.getFloor());
        response.setArea(room.getArea());
        response.setRentPrice(room.getRentPrice());
        response.setDeposit(room.getDeposit());
        response.setCapacity(room.getCapacity());
        response.setElectricityPrice(room.getElectricityPrice());
        response.setWaterPrice(room.getWaterPrice());
        response.setInternetFee(room.getInternetFee());
        response.setServiceFee(room.getServiceFee());
        int computedScore = healthScoreService != null
                ? healthScoreService.calculateRoomHealthScore(room).getTotalScore()
                : (room.getHealthScore() != null ? room.getHealthScore() : 100);
        response.setHealthScore(computedScore);
        response.setAmenities(room.getAmenities());
        response.setDescription(room.getDescription());
        response.setImageUrl(room.getImageUrl());
        response.setStatus(room.getStatus());

        String tenantLabel = null;
        if (bookingRequestRepository != null) {
            Optional<BookingRequest> approvedBooking = bookingRequestRepository
                    .findFirstByRoomIdAndRequestTypeAndStatusOrderByUpdatedAtDesc(
                            room.getId(), BookingRequest.RequestType.BOOKING, BookingRequest.Status.APPROVED);
            if (approvedBooking.isPresent()) {
                BookingRequest b = approvedBooking.get();
                tenantLabel = b.getTenantName() + " (" + b.getTenantPhone() + ")";
            }
        }
        if (tenantLabel == null) {
            List<Invoice> roomInvoices = invoiceRepository.findByRoomIdOrderByBillingMonthDesc(room.getId());
            tenantLabel = roomInvoices.stream()
                    .map(Invoice::getTenant)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .map(t -> t.getFullName() + " (" + t.getPhone() + ")")
                    .orElse(null);
        }
        response.setCurrentContract(room.getStatus() == RoomStatus.VACANT ? null : "Đang hiệu lực");
        response.setCurrentTenant(tenantLabel);
        response.setCreatedAt(room.getCreatedAt());
        response.setUpdatedAt(room.getUpdatedAt());
        return response;
    }
}
