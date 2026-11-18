package com.roomcare.backend.dto.tenant;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MyContractResponse(Long id, String status, LocalDate startDate, LocalDate endDate,
                                 BigDecimal rentPrice, BigDecimal deposit, String terms) {
    public static MyContractResponse empty() {
        return new MyContractResponse(null, null, null, null, null, null, null);
    }
}
