package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.room.CreateRoomRequest;
import com.roomcare.backend.dto.room.RoomResponse;
import com.roomcare.backend.dto.room.UpdateRoomRequest;
import com.roomcare.backend.entity.Invoice;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.Room;
import com.roomcare.backend.entity.RoomStatus;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {
    
    private final RoomRepository roomRepository;
    private final PropertyRepository propertyRepository;
    private final InvoiceRepository invoiceRepository;
    
    @Transactional
    public RoomResponse createRoom(Long landlordId, CreateRoomRequest request) {
        // Kiểm tra property tồn tại và thuộc về landlord
        Property property = propertyRepository.findById(request.getPropertyId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà trọ"));
        
        if (!property.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền thêm phòng vào nhà trọ này");
        }
        
        // Kiểm tra mã phòng đã tồn tại trong property
        if (roomRepository.existsByPropertyIdAndRoomCode(request.getPropertyId(), request.getRoomCode())) {
            throw new RuntimeException("Mã phòng đã tồn tại trong nhà trọ này");
        }
        
        // Validate business rules
        if (request.getRentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá thuê phải lớn hơn 0");
        }
        
        if (request.getDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Tiền đặt cọc phải lớn hơn hoặc bằng 0");
        }
        
        Room room = new Room();
        room.setProperty(property);
        room.setRoomCode(request.getRoomCode());
        room.setFloor(request.getFloor());
        room.setArea(request.getArea());
        room.setRentPrice(request.getRentPrice());
        room.setDeposit(request.getDeposit());
        room.setCapacity(request.getCapacity());
        room.setAmenities(request.getAmenities());
        room.setDescription(request.getDescription());
        room.setImageUrl(request.getImageUrl());
        room.setStatus(RoomStatus.VACANT); // Mặc định phòng trống khi tạo mới
        
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
        
        // Kiểm tra property thuộc về landlord
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
        
        // Kiểm tra mã phòng mới nếu thay đổi
        if (!room.getRoomCode().equals(request.getRoomCode())) {
            if (roomRepository.existsByPropertyIdAndRoomCode(
                    room.getProperty().getId(), request.getRoomCode())) {
                throw new RuntimeException("Mã phòng đã tồn tại trong nhà trọ này");
            }
        }
        
        // Validate business rules
        if (request.getRentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá thuê phải lớn hơn 0");
        }
        
        if (request.getDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Tiền đặt cọc phải lớn hơn hoặc bằng 0");
        }
        
        room.setRoomCode(request.getRoomCode());
        room.setFloor(request.getFloor());
        room.setArea(request.getArea());
        room.setRentPrice(request.getRentPrice());
        room.setDeposit(request.getDeposit());
        room.setCapacity(request.getCapacity());
        room.setAmenities(request.getAmenities());
        room.setDescription(request.getDescription());
        room.setImageUrl(request.getImageUrl());
        room.setStatus(request.getStatus());
        
        Room updated = roomRepository.save(room);
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

    private RoomResponse mapToResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setPropertyId(room.getProperty().getId());
        response.setPropertyName(room.getProperty().getName());
        response.setRoomCode(room.getRoomCode());
        response.setFloor(room.getFloor());
        response.setArea(room.getArea());
        response.setRentPrice(room.getRentPrice());
        response.setDeposit(room.getDeposit());
        response.setCapacity(room.getCapacity());
        response.setAmenities(room.getAmenities());
        response.setDescription(room.getDescription());
        response.setImageUrl(room.getImageUrl());
        response.setStatus(room.getStatus());
        List<Invoice> roomInvoices = invoiceRepository.findByRoomIdOrderByBillingMonthDesc(room.getId());
        String tenantLabel = roomInvoices.stream()
                .map(Invoice::getTenant)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .map(t -> t.getFullName() + " (" + t.getPhone() + ")")
                .orElse(null);
        response.setCurrentContract(room.getStatus() == RoomStatus.VACANT ? null : "Đang hiệu lực");
        response.setCurrentTenant(tenantLabel);
        response.setCreatedAt(room.getCreatedAt());
        response.setUpdatedAt(room.getUpdatedAt());
        return response;
    }
}
