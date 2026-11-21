package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.tenant.CreateTenantRequest;
import com.roomcare.backend.dto.tenant.TenantResponse;
import com.roomcare.backend.dto.tenant.UpdateTenantRequest;
import com.roomcare.backend.entity.BookingRequest;
import com.roomcare.backend.entity.Contract;
import com.roomcare.backend.entity.ContractStatus;
import com.roomcare.backend.entity.Invoice;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.Role;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.BookingRequestRepository;
import com.roomcare.backend.repository.ContractRepository;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.RoommateRepository;
import com.roomcare.backend.repository.TenantRepository;
import com.roomcare.backend.repository.UserRepository;
import com.roomcare.backend.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    @Autowired(required = false)
    private ContractRepository contractRepository;

    @Autowired(required = false)
    private RoommateRepository roommateRepository;
    
    @Transactional
    public TenantResponse createTenant(Long landlordId, CreateTenantRequest request) {
        Property property = propertyRepository.findById(request.getPropertyId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà trọ"));
        
        if (!property.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền thêm khách thuê vào nhà trọ này");
        }
        
        User landlord = userRepository.findById(landlordId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));

        String normalizedPhone = normalizePhone(request.getPhone());
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (normalizedPhone == null && normalizedEmail == null) {
            throw new RuntimeException("Vui lòng nhập ít nhất số điện thoại hoặc email cho khách thuê");
        }

        if (normalizedPhone != null && existsPhoneInTenants(normalizedPhone)) {
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        }
        
        String normalizedId = request.getIdentityNumber().trim();
        if (tenantRepository.existsByIdentityNumber(normalizedId)) {
            throw new RuntimeException("Số CMND/CCCD đã được sử dụng");
        }
        
        Tenant tenant = new Tenant();
        tenant.setLandlord(landlord);
        tenant.setProperty(property);
        tenant.setFullName(request.getFullName().trim());
        tenant.setPhone(normalizedPhone);
        tenant.setEmail(normalizedEmail);
        tenant.setIdentityNumber(normalizedId);
        tenant.setDateOfBirth(request.getDateOfBirth());
        tenant.setAddress(trimOrNull(request.getAddress()));
        tenant.setEmergencyContact(trimOrNull(request.getEmergencyContact()));
        tenant.setUser(resolveMatchingTenantUser(normalizedEmail, normalizedPhone).orElse(null));
        
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
        
        String normalizedPhone = normalizePhone(request.getPhone());
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (normalizedPhone == null && normalizedEmail == null) {
            throw new RuntimeException("Vui lòng nhập ít nhất số điện thoại hoặc email cho khách thuê");
        }

        if (normalizedPhone != null && !Objects.equals(tenant.getPhone(), normalizedPhone)) {
            if (existsPhoneInTenants(normalizedPhone)) {
                throw new RuntimeException("Số điện thoại đã được sử dụng");
            }
        }
        
        String normalizedId = request.getIdentityNumber().trim();
        if (!tenant.getIdentityNumber().equals(normalizedId)) {
            if (tenantRepository.existsByIdentityNumber(normalizedId)) {
                throw new RuntimeException("Số CMND/CCCD đã được sử dụng");
            }
        }
        
        tenant.setFullName(request.getFullName().trim());
        tenant.setPhone(normalizedPhone);
        tenant.setEmail(normalizedEmail);
        tenant.setIdentityNumber(normalizedId);
        tenant.setDateOfBirth(request.getDateOfBirth());
        tenant.setAddress(trimOrNull(request.getAddress()));
        tenant.setEmergencyContact(trimOrNull(request.getEmergencyContact()));
        if (tenant.getUser() == null) {
            resolveMatchingTenantUser(normalizedEmail, normalizedPhone).ifPresent(tenant::setUser);
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

        if (contractRepository != null && contractRepository.findByTenantIdOrderByStartDateDescCreatedAtDesc(tenantId)
                .stream()
                .anyMatch(c -> c.getStatus() == ContractStatus.ACTIVE || c.getStatus() == ContractStatus.SIGNED)) {
            throw new RuntimeException("Không thể xóa khách thuê đang có hợp đồng thuê hiệu lực hoặc đã ký");
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
        response.setAccountStatus(ContractService.resolveAccountStatus(tenant));
        response.setRoommateCount(roommateRepository != null
                ? (int) roommateRepository.countByTenantId(tenant.getId())
                : 0);

        Long resolvedRoomId = null;
        String resolvedRoomCode = null;
        Long resolvedContractId = null;
        String resolvedContractStatus = null;
        String resolvedContractLabel = null;

        if (contractRepository != null) {
            Optional<Contract> primaryContract = contractRepository
                    .findFirstByTenantIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(
                            tenant.getId(), List.of(ContractStatus.ACTIVE, ContractStatus.SIGNED));
            if (primaryContract.isEmpty()) {
                primaryContract = contractRepository.findByTenantIdOrderByStartDateDescCreatedAtDesc(tenant.getId())
                        .stream().findFirst();
            }
            if (primaryContract.isPresent()) {
                Contract c = primaryContract.get();
                resolvedRoomId = c.getRoom().getId();
                resolvedRoomCode = c.getRoom().getRoomCode();
                resolvedContractId = c.getId();
                resolvedContractStatus = c.getStatus().name();
                resolvedContractLabel = c.getContractCode() + " (" + c.getStatus().name() + ") - Phòng " + resolvedRoomCode;
            }
        }
        if (resolvedRoomCode == null && bookingRequestRepository != null && tenant.getUser() != null) {
            Optional<BookingRequest> approved = bookingRequestRepository
                    .findFirstByTenantUserIdAndRequestTypeAndStatusOrderByUpdatedAtDesc(
                            tenant.getUser().getId(),
                            BookingRequest.RequestType.BOOKING,
                            BookingRequest.Status.APPROVED
                    );
            if (approved.isPresent()) {
                resolvedRoomId = approved.get().getRoom().getId();
                resolvedRoomCode = approved.get().getRoom().getRoomCode();
            }
        }
        if (resolvedRoomCode == null) {
            List<Invoice> tenantInvoices = invoiceRepository.findByTenantIdOrderByBillingMonthDescCreatedAtDesc(tenant.getId());
            if (!tenantInvoices.isEmpty()) {
                resolvedRoomId = tenantInvoices.get(0).getRoom().getId();
                resolvedRoomCode = tenantInvoices.get(0).getRoom().getRoomCode();
            }
        }
        response.setCurrentRoomId(resolvedRoomId);
        response.setCurrentContractId(resolvedContractId);
        response.setCurrentContractStatus(resolvedContractStatus);
        if (resolvedRoomCode != null) {
            response.setCurrentRoom("Phòng " + resolvedRoomCode);
            response.setCurrentContract(resolvedContractLabel != null
                    ? resolvedContractLabel
                    : "Đang hiệu lực - Phòng " + resolvedRoomCode);
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

    private boolean existsPhoneInTenants(String phone) {
        List<String> variants = PhoneNumberUtils.lookupVariants(phone);
        return variants.isEmpty() ? tenantRepository.existsByPhone(phone) : tenantRepository.existsByPhoneIn(variants);
    }

    private String normalizePhone(String rawPhone) {
        String trimmed = trimOrNull(rawPhone);
        if (trimmed == null) return null;
        return PhoneNumberUtils.isValidVietnamMobile(trimmed)
                ? PhoneNumberUtils.normalizeToE164(trimmed)
                : trimmed;
    }

    private String normalizeEmail(String rawEmail) {
        String trimmed = trimOrNull(rawEmail);
        return trimmed != null ? trimmed.toLowerCase(Locale.ROOT) : null;
    }

    private String trimOrNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
