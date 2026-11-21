package com.roomcare.backend.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminAccountResponse(
        Long id,
        String accountCode,
        String fullName,
        String email,
        String phone,
        String role,
        boolean isActive,
        boolean emailVerified,
        boolean phoneVerified,
        LocalDateTime createdAt,
        Long landlordId,
        String landlordName,
        String landlordCode,
        Long propertyId,
        String propertyName,
        Long roomId,
        String roomCode,
        boolean hasTenantLink,
        String noteMessage
) {
    public record PageResult(
            List<AdminAccountResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages,
            long allCount,
            long adminCount,
            long landlordCount,
            long tenantCount,
            long activeCount,
            long lockedCount
    ) {}
}
