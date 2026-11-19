package com.roomcare.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LandlordDashboardResponse {
    private int totalProperties;
    private int totalRooms;
    private int occupiedRooms;
    private int vacantRooms;
    private int expiringSoonRooms;
    private int issueRooms;
    private BigDecimal totalRevenue;
    private BigDecimal collectedAmount;
    private BigDecimal uncollectedAmount;
    private double collectionRate;
    private int averageHealthScore;
    private int pendingBookingCount;
    private List<MonthlyRevenueResponse> monthlyRevenues = new ArrayList<>();
    private List<AttentionItemResponse> expiringContracts = new ArrayList<>();
    private List<AttentionItemResponse> attentionRooms = new ArrayList<>();
}
