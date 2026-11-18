package com.roomcare.backend.dto.tenant;

import java.math.BigDecimal;

public record TenantPaymentInfoResponse(String bankName, String accountNumber, BigDecimal amount,
                                        String transferContent, String qrPayload) {}
