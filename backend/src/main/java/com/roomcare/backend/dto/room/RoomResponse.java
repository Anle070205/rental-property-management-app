package com.roomcare.backend.dto.room;

import com.roomcare.backend.entity.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private Long propertyId;
    private String propertyName;
    private String roomCode;
    private Integer floor;
    private BigDecimal area;
    private BigDecimal rentPrice;
    private BigDecimal deposit;
    private Integer capacity;
    private String amenities;
    private String description;
    private String imageUrl;
    private RoomStatus status;
    private String currentContract; // TODO: Phase 3 - thông tin hợp đồng hiện tại
    private String currentTenant; // TODO: Phase 3 - tên khách thuê hiện tại
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
