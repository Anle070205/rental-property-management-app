package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.incident.*;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.IncidentRepository;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentRepository incidents;
    private final TenantRepository tenants;
    private final NotificationService notifications;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> list(User user, Incident.Status status, int page, int size) {
        Long tenantId = tenantFor(user).getId();
        if (page < 0 || size < 1 || size > 100) {
            throw new RuntimeException("Trang phải từ 0 và kích thước từ 1 đến 100");
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = status == null ? incidents.findByTenantId(tenantId, pageable)
                : incidents.findByTenantIdAndStatus(tenantId, status, pageable);
        return PageResponse.of(result.map(IncidentResponse::from).getContent(), page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(User user, Long id) {
        return incidents.findByIdAndTenantId(id, tenantFor(user).getId()).map(IncidentResponse::from)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố"));
    }

    @Transactional
    public IncidentResponse create(User user, CreateIncidentRequest request) {
        Tenant tenant = tenantFor(user);
        Incident incident = new Incident();
        incident.setTenant(tenant);
        if (invoiceRepository != null) {
            invoiceRepository.findByTenantIdOrderByBillingMonthDescCreatedAtDesc(tenant.getId())
                    .stream()
                    .findFirst()
                    .ifPresent(inv -> incident.setRoom(inv.getRoom()));
        }
        incident.setType(request.type());
        incident.setTitle(request.title().trim());
        incident.setDescription(request.description().trim());
        incident.setPriority(request.priority());
        incident.setImageUrl(request.imageUrl());
        incidents.saveAndFlush(incident);
        notifications.createNotification(tenant.getLandlord(), NotificationType.INCIDENT,
                "Có báo cáo sự cố mới", tenant.getFullName() + ": " + incident.getTitle(), incident.getId());
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse supplement(User user, Long id, UpdateIncidentRequest request) {
        Incident incident = incidents.findForUpdate(id, tenantFor(user).getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố"));
        if (incident.getStatus() == Incident.Status.DONE) {
            throw new RuntimeException("Sự cố đã hoàn tất");
        }
        boolean hasDescription = request.description() != null && !request.description().isBlank();
        if (!hasDescription && request.imageUrl() == null) {
            throw new RuntimeException("Vui lòng nhập mô tả bổ sung hoặc liên kết ảnh");
        }
        if (hasDescription) {
            String description = incident.getDescription() + "\n\n" + request.description().trim();
            if (description.length() > 12000) {
                throw new RuntimeException("Tổng nội dung mô tả tối đa 12000 ký tự");
            }
            incident.setDescription(description);
        }
        if (request.imageUrl() != null) {
            incident.setImageUrl(request.imageUrl());
        }
        incidents.flush();
        return IncidentResponse.from(incident);
    }

    private Tenant tenantFor(User user) {
        return tenants.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Tài khoản chưa được liên kết với người thuê"));
    }
}
