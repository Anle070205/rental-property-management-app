package com.roomcare.backend.service;

import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.tenant.MyContractResponse;
import com.roomcare.backend.dto.tenant.MyRoomResponse;
import com.roomcare.backend.dto.tenant.SubmitPaymentProofRequest;
import com.roomcare.backend.dto.tenant.TenantDashboardResponse;
import com.roomcare.backend.dto.tenant.TenantInvoiceResponse;
import com.roomcare.backend.dto.tenant.TenantPaymentInfoResponse;
import com.roomcare.backend.dto.tenant.TenantPaymentResponse;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.TenantRepository;
import java.util.List;
import java.util.Objects;
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
@Transactional
public class TenantPortalService {
    private final TenantRepository tenants;
    private final NotificationService notifications;
    private final ObjectProvider<TenantPortalProvider> provider;

    public TenantDashboardResponse dashboard(User user) {
        Optional<Tenant> tenantOpt = optionalTenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        if (tenantOpt.isEmpty() || source == null) {
            return new TenantDashboardResponse(user.getFullName(), null, null, null, notifications.unreadCount(user));
        }
        Tenant tenant = tenantOpt.get();
        var amountDue = source.amountDue(tenant).orElse(null);
        return new TenantDashboardResponse(
                user.getFullName(),
                source.activeRoomCode(tenant).orElse(null),
                amountDue == null ? null : amountDue.amount(),
                amountDue == null ? null : amountDue.dueDate(),
                notifications.unreadCount(user)
        );
    }

    public MyRoomResponse room(User user) {
        Optional<Tenant> tenantOpt = optionalTenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        if (tenantOpt.isEmpty() || source == null) {
            return MyRoomResponse.empty();
        }
        return source.room(tenantOpt.get()).orElseGet(MyRoomResponse::empty);
    }

    public MyContractResponse contract(User user) {
        Optional<Tenant> tenantOpt = optionalTenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        if (tenantOpt.isEmpty() || source == null) {
            return MyContractResponse.empty();
        }
        return source.contract(tenantOpt.get()).orElseGet(MyContractResponse::empty);
    }

    public boolean requestRenewal(User user, Long contractId) {
        Optional<Tenant> tenantOpt = optionalTenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        return tenantOpt.isPresent() && source != null && source.requestRenewal(tenantOpt.get(), contractId);
    }

    public PageResponse<TenantInvoiceResponse> invoices(User user, String month, String status, int page, int size) {
        if (month != null && !month.matches("[0-9]{4}-(0[1-9]|1[0-2])")) {
            throw new RuntimeException("Tháng phải theo định dạng YYYY-MM");
        }
        if (status != null && !Set.of("UNPAID", "PARTIALLY_PAID", "PAID", "OVERDUE").contains(status)) {
            throw new RuntimeException("Trạng thái hóa đơn không hợp lệ");
        }
        var pageable = pageRequest(page, size);
        Optional<Tenant> tenantOpt = optionalTenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        if (tenantOpt.isEmpty() || source == null) {
            return PageResponse.of(List.of(), page, size, 0, 0);
        }
        Page<TenantInvoiceResponse> result = source.invoices(tenantOpt.get(), month, status, pageable);
        return PageResponse.of(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    public Optional<TenantInvoiceResponse> invoice(User user, Long invoiceId) {
        Optional<Tenant> tenantOpt = optionalTenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        if (tenantOpt.isEmpty() || source == null) {
            return Optional.empty();
        }
        return source.invoice(tenantOpt.get(), invoiceId);
    }

    public Optional<TenantInvoiceResponse> submitPaymentProof(
            User user,
            Long invoiceId,
            SubmitPaymentProofRequest request) {
        Optional<Tenant> tenantOpt = optionalTenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        if (tenantOpt.isEmpty() || source == null) {
            return Optional.empty();
        }
        return source.submitPaymentProof(tenantOpt.get(), invoiceId, request);
    }

    public Optional<TenantPaymentInfoResponse> paymentInfo(User user, Long invoiceId) {
        Optional<Tenant> tenantOpt = optionalTenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        if (tenantOpt.isEmpty() || source == null) {
            return Optional.empty();
        }
        Tenant tenant = tenantOpt.get();
        return source.invoice(tenant, invoiceId)
                .filter(invoice -> invoice.remainingAmount().signum() > 0)
                .flatMap(invoice -> source.paymentInfo(tenant, invoiceId));
    }

    public PageResponse<TenantPaymentResponse> payments(User user, int page, int size) {
        var pageable = pageRequest(page, size);
        Optional<Tenant> tenantOpt = optionalTenantFor(user);
        TenantPortalProvider source = provider.getIfAvailable();
        if (tenantOpt.isEmpty() || source == null) {
            return PageResponse.of(List.of(), page, size, 0, 0);
        }
        Page<TenantPaymentResponse> result = source.payments(tenantOpt.get(), pageable);
        return PageResponse.of(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new RuntimeException("Trang phải từ 0 và kích thước từ 1 đến 100");
        }
        return PageRequest.of(page, size);
    }

    private Optional<Tenant> optionalTenantFor(User user) {
        return tenants.findByUserId(user.getId())
                .or(() -> {
                    Optional<Tenant> byEmailOrPhone = tenants.findFirstByEmailIgnoreCaseOrPhone(
                            user.getEmail(), user.getPhone()
                    );
                    if (byEmailOrPhone.isEmpty() && user.getPhone() != null && !user.getPhone().isBlank()) {
                        byEmailOrPhone = tenants.findFirstByPhoneIn(
                                com.roomcare.backend.util.PhoneNumberUtils.lookupVariants(user.getPhone())
                        );
                    }
                    return byEmailOrPhone
                            .filter(found -> found.getUser() == null || Objects.equals(found.getUser().getId(), user.getId()))
                            .map(found -> {
                                if (found.getUser() == null) {
                                    found.setUser(user);
                                    return tenants.save(found);
                                }
                                return found;
                            });
                });
    }
}
