package com.roomcare.backend.service;

import com.roomcare.backend.dto.tenant.MyContractResponse;
import com.roomcare.backend.dto.tenant.MyRoomResponse;
import com.roomcare.backend.dto.tenant.TenantInvoiceResponse;
import com.roomcare.backend.entity.Tenant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Implement this interface with queries scoped to the authenticated tenant. */
public interface TenantPortalProvider {
    record AmountDue(BigDecimal amount, LocalDate dueDate) {}

    default Optional<String> activeRoomCode(Tenant tenant) {
        return room(tenant).map(MyRoomResponse::roomCode);
    }

    default Optional<MyRoomResponse> room(Tenant tenant) {
        return Optional.empty();
    }

    default Optional<MyContractResponse> contract(Tenant tenant) {
        return Optional.empty();
    }

    /** Store a renewal request only if the contract belongs to the tenant and is eligible. */
    default boolean requestRenewal(Tenant tenant, Long contractId) {
        return false;
    }

    default Page<TenantInvoiceResponse> invoices(Tenant tenant, String month, String status, Pageable pageable) {
        return Page.empty(pageable);
    }

    default Optional<TenantInvoiceResponse> invoice(Tenant tenant, Long invoiceId) {
        return Optional.empty();
    }

    default Optional<AmountDue> amountDue(Tenant tenant) {
        return Optional.empty();
    }
}
