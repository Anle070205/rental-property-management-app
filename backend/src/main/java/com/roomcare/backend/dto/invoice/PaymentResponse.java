package com.roomcare.backend.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long invoiceId;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionCode;
    private String receiptNote;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
