package com.roomcare.backend.dto.utility;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilityRecordResponse {
    private Long id;
    private Long propertyId;
    private String propertyName;
    private Long roomId;
    private String roomCode;
    private String billingMonth;
    private BigDecimal prevElectricIndex;
    private BigDecimal currElectricIndex;
    private BigDecimal electricUsage;
    private BigDecimal electricUnitPrice;
    private BigDecimal electricityAmount;
    private BigDecimal prevWaterIndex;
    private BigDecimal currWaterIndex;
    private BigDecimal waterUsage;
    private BigDecimal waterUnitPrice;
    private BigDecimal waterAmount;
    private BigDecimal internetFee;
    private BigDecimal serviceFee;
    private BigDecimal totalUtilityAmount;
    private boolean isAdjustment;
    private String adjustmentReason;
    private String notes;
    private String historyLog;
    private Long linkedInvoiceId;
    private String linkedInvoiceCode;
    private boolean invoiceLocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
