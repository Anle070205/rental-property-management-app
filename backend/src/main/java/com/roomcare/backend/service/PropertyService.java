package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.property.CreatePropertyRequest;
import com.roomcare.backend.dto.property.PropertyResponse;
import com.roomcare.backend.dto.property.UpdatePropertyRequest;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.RoomStatus;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.RoomRepository;
import com.roomcare.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {
    
    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public PropertyResponse createProperty(Long landlordId, CreatePropertyRequest request) {
        User landlord = userRepository.findById(landlordId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));
        
        Property property = new Property();
        property.setLandlord(landlord);
        property.setName(request.getName());
        property.setAddress(request.getAddress());
        property.setDescription(request.getDescription());
        
        Property saved = propertyRepository.save(property);
        return mapToResponse(saved, 0, 0, 0);
    }
    
    @Transactional(readOnly = true)
    public PageResponse<PropertyResponse> getPropertiesByLandlord(
            Long landlordId, 
            int page, 
            int size, 
            String sortBy, 
            String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Property> propertyPage = propertyRepository.findByLandlordId(landlordId, pageable);
        
        List<PropertyResponse> content = propertyPage.getContent().stream()
            .map(this::mapToResponseWithRoomStats)
            .collect(Collectors.toList());
        
        return PageResponse.of(
            content,
            propertyPage.getNumber(),
            propertyPage.getSize(),
            propertyPage.getTotalElements(),
            propertyPage.getTotalPages()
        );
    }
    
    @Transactional(readOnly = true)
    public PropertyResponse getPropertyById(Long landlordId, Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà trọ"));
        
        if (!property.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền truy cập nhà trọ này");
        }
        
        return mapToResponseWithRoomStats(property);
    }
    
    @Transactional
    public PropertyResponse updateProperty(Long landlordId, Long propertyId, UpdatePropertyRequest request) {
        Property property = propertyRepository.findById(propertyId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà trọ"));
        
        if (!property.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật nhà trọ này");
        }
        
        property.setName(request.getName());
        property.setAddress(request.getAddress());
        property.setDescription(request.getDescription());
        
        Property updated = propertyRepository.save(property);
        return mapToResponseWithRoomStats(updated);
    }
    
    private PropertyResponse mapToResponse(Property property, int totalRooms, int vacantRooms, int occupiedRooms) {
        PropertyResponse response = new PropertyResponse();
        response.setId(property.getId());
        response.setLandlordId(property.getLandlord().getId());
        response.setLandlordName(property.getLandlord().getFullName());
        response.setName(property.getName());
        response.setAddress(property.getAddress());
        response.setDescription(property.getDescription());
        response.setTotalRooms(totalRooms);
        response.setVacantRooms(vacantRooms);
        response.setOccupiedRooms(occupiedRooms);
        response.setCreatedAt(property.getCreatedAt());
        response.setUpdatedAt(property.getUpdatedAt());
        return response;
    }
    
    private PropertyResponse mapToResponseWithRoomStats(Property property) {
        Page<com.roomcare.backend.entity.Room> rooms = roomRepository.findByPropertyId(
            property.getId(), 
            PageRequest.of(0, Integer.MAX_VALUE)
        );
        
        int totalRooms = (int) rooms.getTotalElements();
        int vacantRooms = (int) rooms.getContent().stream()
            .filter(room -> room.getStatus() == RoomStatus.VACANT)
            .count();
        int occupiedRooms = (int) rooms.getContent().stream()
            .filter(room -> room.getStatus() == RoomStatus.OCCUPIED)
            .count();
        
        return mapToResponse(property, totalRooms, vacantRooms, occupiedRooms);
    }
}
