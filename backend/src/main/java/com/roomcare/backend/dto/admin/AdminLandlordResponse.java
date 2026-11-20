package com.roomcare.backend.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminLandlordResponse(
        Long id,
        String landlordCode,
        String fullName,
        String email,
        String phone,
        boolean isActive,
        LocalDateTime createdAt,
        String address,
        long propertyCount,
        long roomCount,
        long tenantCount,
        List<PropertySummary> properties,
        List<TenantSummary> tenants
) {
    public record PropertySummary(Long id, String name, long roomCount, long occupiedCount, long tenantCount) {}
    public record TenantSummary(Long tenantId, Long userId, String fullName, String propertyName, String roomCode) {}
    public record ListResult(List<AdminLandlordResponse> items, long totalCount, long activeCount, long lockedCount) {}
}
