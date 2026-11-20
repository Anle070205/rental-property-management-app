package com.roomcare.backend.dto.invoice;

import com.roomcare.backend.entity.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String invoiceCode;
    private Long propertyId;
    private String propertyName;
    private Long roomId;
    private String roomCode;
    private Long tenantId;
    private String tenantName;
    private String tenantPhone;
    private Long contractId;
    private String contractCode;
    private Long utilityRecordId;
    private String billingMonth;
    private BigDecimal roomRent;
    private BigDecimal electricityAmount;
    private BigDecimal waterAmount;
    private BigDecimal internetAmount;
    private BigDecimal garbageFee;
    private BigDecimal serviceAmount;
    private BigDecimal otherFees;
    private BigDecimal previousDebt;
    private BigDecimal adjustmentAmount;
    private String adjustmentReason;
    private String adjustmentHistory;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private InvoiceStatus status;
    private LocalDate dueDate;
    private long daysOverdue;
    private String notes;
    private BigDecimal prevElectricIndex;
    private BigDecimal currElectricIndex;
    private BigDecimal electricUsage;
    private BigDecimal electricUnitPrice;
    private BigDecimal prevWaterIndex;
    private BigDecimal currWaterIndex;
    private BigDecimal waterUsage;
    private BigDecimal waterUnitPrice;
    private String waterBillingMode;
    private Integer occupantCount;
    private LocalDateTime publishedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private String paymentProofNote;
    private String paymentProofUrl;
    private LocalDateTime paymentProofSubmittedAt;
    private List<PaymentResponse> payments = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
