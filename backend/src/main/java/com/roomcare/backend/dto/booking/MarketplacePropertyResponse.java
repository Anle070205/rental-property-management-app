package com.roomcare.backend.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketplacePropertyResponse {
    private Long id;
    private String name;
    private String address;
    private String description;
    private Long landlordId;
    private String landlordName;
    private String landlordPhone;
    private String landlordEmail;
    private int totalRooms;
    private int vacantRooms;
    private BigDecimal minRentPrice;
    private BigDecimal maxRentPrice;
    private int averageHealthScore;
}
