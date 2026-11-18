package com.roomcare.backend.service;

import com.roomcare.backend.dto.tenant.MyContractResponse;
import com.roomcare.backend.dto.tenant.MyRoomResponse;
import com.roomcare.backend.entity.Tenant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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

    default Optional<AmountDue> amountDue(Tenant tenant) {
        return Optional.empty();
    }
}
