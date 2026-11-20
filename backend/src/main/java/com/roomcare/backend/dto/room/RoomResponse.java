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
    private String propertyAddress;
    private String landlordName;
    private String landlordPhone;
    private String roomCode;
    private Integer floor;
    private BigDecimal area;
    private BigDecimal rentPrice;
    private BigDecimal deposit;
    private Integer capacity;
    private BigDecimal electricityPrice;
    private BigDecimal waterPrice;
    private BigDecimal internetFee;
    private BigDecimal serviceFee;
    private Integer healthScore;
    private String amenities;
    private String description;
    private String imageUrl;
    private RoomStatus status;
    private String currentContract;
    private String currentTenant;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
