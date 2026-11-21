package com.roomcare.backend.dto.incident;

import com.roomcare.backend.entity.Incident;
import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        Long roomId,
        Incident.Type type,
        String title,
        String description,
        Incident.Priority priority,
        Incident.Status status,
        String imageUrl,
        String landlordResponse,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String roomCode,
        String tenantName
) {
    public IncidentResponse(
            Long id,
            Long roomId,
            Incident.Type type,
            String title,
            String description,
            Incident.Priority priority,
            Incident.Status status,
            String imageUrl,
            String landlordResponse,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, roomId, type, title, description, priority, status, imageUrl, landlordResponse,
                createdAt, updatedAt, null, null);
    }

    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getRoom() == null ? null : incident.getRoom().getId(),
                incident.getType(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getPriority(),
                incident.getStatus(),
                incident.getImageUrl(),
                incident.getLandlordResponse(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getRoom() == null ? null : incident.getRoom().getRoomCode(),
                incident.getTenant() == null ? null : incident.getTenant().getFullName()
        );
    }
}
