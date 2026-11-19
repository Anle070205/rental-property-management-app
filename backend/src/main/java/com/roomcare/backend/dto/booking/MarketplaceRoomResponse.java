package com.roomcare.backend.dto.booking;

import com.roomcare.backend.entity.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceRoomResponse {
    private Long id;
    private Long propertyId;
    private String propertyName;
    private String propertyAddress;
    private Long landlordId;
    private String landlordName;
    private String landlordPhone;
    private String landlordEmail;
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
    private String amenities;
    private String description;
    private String imageUrl;
    private RoomStatus status;
    private Integer healthScore;
    private String healthRatingLabel;
    private boolean hasPendingBookingByMe;
}
