package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.tenant.CreateTenantRequest;
import com.roomcare.backend.dto.tenant.TenantResponse;
import com.roomcare.backend.dto.tenant.UpdateTenantRequest;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.TenantRepository;
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
public class TenantService {
    
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public TenantResponse createTenant(Long landlordId, CreateTenantRequest request) {
        // Kiểm tra property tồn tại và thuộc về landlord
        Property property = propertyRepository.findById(request.getPropertyId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà trọ"));
        
        if (!property.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền thêm khách thuê vào nhà trọ này");
        }
        
        User landlord = userRepository.findById(landlordId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));
        
        // Kiểm tra số điện thoại đã tồn tại
        if (tenantRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        }
        
        // Kiểm tra số CMND/CCCD đã tồn tại
        if (tenantRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            throw new RuntimeException("Số CMND/CCCD đã được sử dụng");
        }
        
        Tenant tenant = new Tenant();
        tenant.setLandlord(landlord);
        tenant.setProperty(property);
        tenant.setFullName(request.getFullName());
        tenant.setPhone(request.getPhone());
        tenant.setEmail(request.getEmail());
        tenant.setIdentityNumber(request.getIdentityNumber());
        tenant.setDateOfBirth(request.getDateOfBirth());
        tenant.setAddress(request.getAddress());
        tenant.setEmergencyContact(request.getEmergencyContact());
        tenant.setUser(null); // Khách thuê chưa có tài khoản
        
        Tenant saved = tenantRepository.save(tenant);
        return mapToResponse(saved, false); // false = không mask CMND khi tạo mới
    }
    
    @Transactional(readOnly = true)
    public PageResponse<TenantResponse> getTenantsByLandlord(
            Long landlordId,
            Long propertyId,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Tenant> tenantPage = tenantRepository.findByLandlordIdWithFilters(
            landlordId, propertyId, keyword, pageable
        );
        
        List<TenantResponse> content = tenantPage.getContent().stream()
            .map(tenant -> mapToResponse(tenant, true)) // true = mask CMND trong danh sách
            .collect(Collectors.toList());
        
        return PageResponse.of(
            content,
            tenantPage.getNumber(),
            tenantPage.getSize(),
            tenantPage.getTotalElements(),
            tenantPage.getTotalPages()
        );
    }
    
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(Long landlordId, Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách thuê"));
        
        if (!tenant.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền xem khách thuê này");
        }
        
        return mapToResponse(tenant, false); // false = không mask CMND khi xem chi tiết
    }
    
    @Transactional
    public TenantResponse updateTenant(Long landlordId, Long tenantId, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách thuê"));
        
        if (!tenant.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật khách thuê này");
        }
        
        // Kiểm tra số điện thoại nếu thay đổi
        if (!tenant.getPhone().equals(request.getPhone())) {
            if (tenantRepository.existsByPhone(request.getPhone())) {
                throw new RuntimeException("Số điện thoại đã được sử dụng");
            }
        }
        
        // Kiểm tra số CMND/CCCD nếu thay đổi
        if (!tenant.getIdentityNumber().equals(request.getIdentityNumber())) {
            if (tenantRepository.existsByIdentityNumber(request.getIdentityNumber())) {
                throw new RuntimeException("Số CMND/CCCD đã được sử dụng");
            }
        }
        
        tenant.setFullName(request.getFullName());
        tenant.setPhone(request.getPhone());
        tenant.setEmail(request.getEmail());
        tenant.setIdentityNumber(request.getIdentityNumber());
        tenant.setDateOfBirth(request.getDateOfBirth());
        tenant.setAddress(request.getAddress());
        tenant.setEmergencyContact(request.getEmergencyContact());
        
        Tenant updated = tenantRepository.save(tenant);
        return mapToResponse(updated, false);
    }
    
    @Transactional
    public void deleteTenant(Long landlordId, Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách thuê"));
        
        if (!tenant.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền xóa khách thuê này");
        }
        
        // TODO: Phase 3 - Kiểm tra khách thuê có hợp đồng đang hoạt động không
        // if (hasActiveContract(tenantId)) {
        //     throw new RuntimeException("Không thể xóa khách thuê đang có hợp đồng hoạt động");
        // }
        
        tenantRepository.delete(tenant);
    }
    
    private TenantResponse mapToResponse(Tenant tenant, boolean maskIdentity) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setUserId(tenant.getUser() != null ? tenant.getUser().getId() : null);
        response.setLandlordId(tenant.getLandlord().getId());
        response.setPropertyId(tenant.getProperty().getId());
        response.setPropertyName(tenant.getProperty().getName());
        response.setFullName(tenant.getFullName());
        response.setPhone(tenant.getPhone());
        response.setEmail(tenant.getEmail());
        response.setIdentityNumber(tenant.getIdentityNumber());
        response.setDateOfBirth(tenant.getDateOfBirth());
        response.setAddress(tenant.getAddress());
        response.setEmergencyContact(tenant.getEmergencyContact());
        response.setCurrentRoom(null); // TODO: Phase 3
        response.setCurrentContract(null); // TODO: Phase 3
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        
        if (maskIdentity) {
            response.maskIdentityNumber();
        }
        
        return response;
    }
}
