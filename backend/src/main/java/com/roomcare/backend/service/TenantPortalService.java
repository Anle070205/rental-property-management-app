package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.tenant.MyContractResponse;
import com.roomcare.backend.dto.tenant.MyRoomResponse;
import com.roomcare.backend.dto.tenant.TenantDashboardResponse;
import com.roomcare.backend.dto.tenant.TenantInvoiceResponse;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.TenantRepository;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantPortalService {
    private final TenantRepository tenants;
    private final NotificationService notifications;
    private final ObjectProvider<TenantPortalProvider> provider;

    public TenantDashboardResponse dashboard(User user) {
        Tenant tenant = tenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        var amountDue = source == null ? null : source.amountDue(tenant).orElse(null);
        return new TenantDashboardResponse(user.getFullName(),
                source == null ? null : source.activeRoomCode(tenant).orElse(null),
                amountDue == null ? null : amountDue.amount(), amountDue == null ? null : amountDue.dueDate(),
                notifications.unreadCount(user));
    }

    public MyRoomResponse room(User user) {
        Tenant tenant = tenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        return source == null ? MyRoomResponse.empty() : source.room(tenant).orElseGet(MyRoomResponse::empty);
    }

    public MyContractResponse contract(User user) {
        Tenant tenant = tenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        return source == null ? MyContractResponse.empty() : source.contract(tenant).orElseGet(MyContractResponse::empty);
    }

    @Transactional
    public boolean requestRenewal(User user, Long contractId) {
        Tenant tenant = tenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        return source != null && source.requestRenewal(tenant, contractId);
    }

    public PageResponse<TenantInvoiceResponse> invoices(User user, String month, String status, int page, int size) {
        Tenant tenant = tenantFor(user);
        if (month != null && !month.matches("[0-9]{4}-(0[1-9]|1[0-2])")) {
            throw new RuntimeException("Tháng phải theo định dạng YYYY-MM");
        }
        if (status != null && !Set.of("UNPAID", "PARTIALLY_PAID", "PAID", "OVERDUE").contains(status)) {
            throw new RuntimeException("Trạng thái hóa đơn không hợp lệ");
        }
        var pageable = pageRequest(page, size);
        TenantPortalProvider source = provider.getIfAvailable();
        Page<TenantInvoiceResponse> result = source == null ? Page.empty(pageable)
                : source.invoices(tenant, month, status, pageable);
        return PageResponse.of(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    public Optional<TenantInvoiceResponse> invoice(User user, Long invoiceId) {
        Tenant tenant = tenantFor(user);
        return Optional.ofNullable(provider.getIfAvailable()).flatMap(source -> source.invoice(tenant, invoiceId));
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new RuntimeException("Trang phải từ 0 và kích thước từ 1 đến 100");
        }
        return PageRequest.of(page, size);
    }

    private Tenant tenantFor(User user) {
        return tenants.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Tài khoản chưa được liên kết với người thuê"));
    }
}
