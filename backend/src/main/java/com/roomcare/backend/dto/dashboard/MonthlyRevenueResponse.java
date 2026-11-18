package com.roomcare.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenueResponse {
    private String billingMonth;
    private String label;
    private BigDecimal totalBilled;
    private BigDecimal totalCollected;
    private BigDecimal totalUnpaid;
}
