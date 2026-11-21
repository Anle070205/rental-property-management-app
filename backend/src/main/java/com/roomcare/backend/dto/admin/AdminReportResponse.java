package com.roomcare.backend.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReportResponse(
        Long id,
        String reportCode,
        String title,
        String description,
        String type,
        String priority,
        String status,
        Long senderUserId,
        String senderName,
        String senderRole,
        String landlordName,
        String propertyName,
        String roomCode,
        String resolutionNote,
        String resolvedByName,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
    public record ListResult(
            List<AdminReportResponse> items,
            long totalCount,
            long pendingCount,
            long inProgressCount,
            long doneCount
    ) {}
}
