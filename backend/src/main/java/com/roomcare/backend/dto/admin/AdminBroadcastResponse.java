package com.roomcare.backend.dto.admin;

import java.time.LocalDateTime;
import java.util.List;

public record AdminBroadcastResponse(
        Long id,
        String broadcastCode,
        String title,
        String content,
        String targetAudience,
        String status,
        int recipientCount,
        String createdByName,
        LocalDateTime sentAt,
        LocalDateTime updatedAt
) {
    public record AudienceCounts(long allActive, long adminActive, long landlordActive, long tenantActive, long lockedCount) {}
    public record ListResult(List<AdminBroadcastResponse> items, long totalCount, long sentCount, long draftCount) {}
}
