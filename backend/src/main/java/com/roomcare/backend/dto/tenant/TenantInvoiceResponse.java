package com.roomcare.backend.dto.tenant;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TenantInvoiceResponse(Long id, String invoiceCode, String billingMonth, String status,
        LocalDate dueDate, BigDecimal roomRent, BigDecimal electricityAmount, BigDecimal waterAmount,
        BigDecimal internetAmount, BigDecimal serviceAmount, BigDecimal otherFees, BigDecimal previousDebt,
        BigDecimal discountAmount, BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal remainingAmount) {}
