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
    private Long utilityRecordId;
    private String billingMonth;
    private BigDecimal roomRent;
    private BigDecimal electricityAmount;
    private BigDecimal waterAmount;
    private BigDecimal internetAmount;
    private BigDecimal serviceAmount;
    private BigDecimal otherFees;
    private BigDecimal previousDebt;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private InvoiceStatus status;
    private LocalDate dueDate;
    private long daysOverdue;
    private String notes;
    private BigDecimal electricUsage;
    private BigDecimal waterUsage;
    private List<PaymentResponse> payments = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
