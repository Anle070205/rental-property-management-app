package com.roomcare.backend.dto.property;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponse {
    private Long id;
    private Long landlordId;
    private String landlordName;
    private String name;
    private String address;
    private String description;
    private Integer totalRooms; // Tổng số phòng
    private Integer vacantRooms; // Số phòng trống
    private Integer occupiedRooms; // Số phòng đã thuê
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
