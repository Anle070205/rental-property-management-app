package com.roomcare.backend.dto.admin;

import java.util.List;

public record AdminPropertyResponse(
        Long id,
        String propertyCode,
        String name,
        String address,
        Long landlordId,
        String landlordName,
        String landlordCode,
        String statusLabel,
        long totalRooms,
        long occupiedRooms,
        long vacantRooms,
        long maintenanceRooms,
        long tenantCount,
        int occupancyPercent,
        List<OccupiedRoomLookup> occupiedRoomLookups
) {
    public record OccupiedRoomLookup(Long roomId, String roomCode, Integer floor, Long tenantUserId, String tenantName) {}
    public record ListResult(
            List<AdminPropertyResponse> items,
            long totalProperties,
            long totalRooms,
            long activeProperties,
            long pausedProperties
    ) {}
}
