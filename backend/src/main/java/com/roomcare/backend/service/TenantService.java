package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.tenant.CreateTenantRequest;
import com.roomcare.backend.dto.tenant.TenantResponse;
import com.roomcare.backend.dto.tenant.UpdateTenantRequest;
import com.roomcare.backend.entity.BookingRequest;
import com.roomcare.backend.entity.Invoice;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.Role;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.BookingRequestRepository;
import com.roomcare.backend.repository.InvoiceRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantService {
    
    private final TenantRepository tenantRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final BookingRequestRepository bookingRequestRepository;
    
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
        tenant.setUser(resolveMatchingTenantUser(request.getEmail(), request.getPhone()).orElse(null));
        
        Tenant saved = tenantRepository.save(tenant);
        return mapToResponse(saved, false); // false = không mask CMND khi tạo mới
    }

    private Optional<User> resolveMatchingTenantUser(String email, String phone) {
        if (email != null && !email.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email.trim());
            if (byEmail.isPresent() && byEmail.get().getRole() == Role.TENANT) {
                return byEmail;
            }
        }
        if (phone != null && !phone.isBlank()) {
            Optional<User> byPhone = userRepository.findFirstByPhoneIn(
                    com.roomcare.backend.util.PhoneNumberUtils.lookupVariants(phone)
            );
            if (byPhone.isPresent() && byPhone.get().getRole() == Role.TENANT) {
                return byPhone;
            }
        }
        return Optional.empty();
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
        if (!java.util.Objects.equals(tenant.getPhone(), request.getPhone())) {
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
        if (tenant.getUser() == null) {
            resolveMatchingTenantUser(request.getEmail(), request.getPhone()).ifPresent(tenant::setUser);
        }
        
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
        
        List<Invoice> tenantInvoices = invoiceRepository.findByTenantIdOrderByBillingMonthDescCreatedAtDesc(tenantId);
        boolean hasUnpaid = tenantInvoices.stream()
                .anyMatch(inv -> inv.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0);
        if (hasUnpaid) {
            throw new RuntimeException("Không thể xóa khách thuê đang còn hóa đơn chưa thanh toán");
        }
        
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
        String resolvedRoomCode = null;
        if (bookingRequestRepository != null && tenant.getUser() != null) {
            resolvedRoomCode = bookingRequestRepository
                    .findFirstByTenantUserIdAndRequestTypeAndStatusOrderByUpdatedAtDesc(
                            tenant.getUser().getId(),
                            BookingRequest.RequestType.BOOKING,
                            BookingRequest.Status.APPROVED
                    )
                    .map(b -> b.getRoom().getRoomCode())
                    .orElse(null);
        }
        if (resolvedRoomCode == null) {
            List<Invoice> tenantInvoices = invoiceRepository.findByTenantIdOrderByBillingMonthDescCreatedAtDesc(tenant.getId());
            if (!tenantInvoices.isEmpty()) {
                resolvedRoomCode = tenantInvoices.get(0).getRoom().getRoomCode();
            }
        }
        if (resolvedRoomCode != null) {
            response.setCurrentRoom("Phòng " + resolvedRoomCode);
            response.setCurrentContract("Đang hiệu lực - Phòng " + resolvedRoomCode);
        } else {
            response.setCurrentRoom(null);
            response.setCurrentContract(null);
        }
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        
        if (maskIdentity) {
            response.maskIdentityNumber();
        }
        
        return response;
    }
}
