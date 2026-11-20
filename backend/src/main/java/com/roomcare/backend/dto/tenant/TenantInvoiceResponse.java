package com.roomcare.backend.dto.tenant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TenantInvoiceResponse(
        Long id,
        String invoiceCode,
        String billingMonth,
        String status,
        LocalDate dueDate,
        BigDecimal roomRent,
        BigDecimal electricityAmount,
        BigDecimal waterAmount,
        BigDecimal internetAmount,
        BigDecimal serviceAmount,
        BigDecimal otherFees,
        BigDecimal previousDebt,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String propertyName,
        String roomCode,
        String tenantName,
        String contractCode,
        BigDecimal prevElectricIndex,
        BigDecimal currElectricIndex,
        BigDecimal electricUsage,
        BigDecimal electricUnitPrice,
        BigDecimal prevWaterIndex,
        BigDecimal currWaterIndex,
        BigDecimal waterUsage,
        BigDecimal waterUnitPrice,
        String waterBillingMode,
        Integer occupantCount,
        BigDecimal garbageFee,
        BigDecimal adjustmentAmount,
        String adjustmentReason,
        String notes,
        LocalDateTime publishedAt,
        String paymentProofNote,
        String paymentProofUrl,
        LocalDateTime paymentProofSubmittedAt,
        List<TenantPaymentResponse> payments
) {
    public TenantInvoiceResponse(
            Long id,
            String invoiceCode,
            String billingMonth,
            String status,
            LocalDate dueDate,
            BigDecimal roomRent,
            BigDecimal electricityAmount,
            BigDecimal waterAmount,
            BigDecimal internetAmount,
            BigDecimal serviceAmount,
            BigDecimal otherFees,
            BigDecimal previousDebt,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal remainingAmount
    ) {
        this(
                id, invoiceCode, billingMonth, status, dueDate,
                roomRent, electricityAmount, waterAmount, internetAmount, serviceAmount,
                otherFees, previousDebt, discountAmount, totalAmount, paidAmount, remainingAmount,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO, null,
                null, null, null, null, null, List.of()
        );
    }
}
