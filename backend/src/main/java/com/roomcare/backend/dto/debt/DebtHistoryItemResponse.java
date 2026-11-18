package com.roomcare.backend.dto.debt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtHistoryItemResponse {
    private String type; // INVOICE | PAYMENT
    private String referenceCode;
    private String billingMonth;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String occurredDate;
    private String description;
}
