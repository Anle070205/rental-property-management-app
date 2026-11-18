package com.roomcare.backend.dto.tenant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TenantPaymentResponse(Long id, Long invoiceId, LocalDateTime paidAt, BigDecimal amount,
                                    String method, String status) {}
