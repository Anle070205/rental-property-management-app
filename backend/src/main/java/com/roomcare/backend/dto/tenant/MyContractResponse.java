package com.roomcare.backend.dto.tenant;

import com.roomcare.backend.dto.roommate.RoommateResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MyContractResponse(
        Long id,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal rentPrice,
        BigDecimal deposit,
        String terms,
        String contractCode,
        String roomCode,
        String propertyName,
        String propertyAddress,
        String landlordName,
        String landlordPhone,
        BigDecimal electricityUnitPrice,
        BigDecimal waterUnitPrice,
        String waterBillingMode,
        BigDecimal internetFee,
        BigDecimal garbageFee,
        BigDecimal serviceFee,
        Integer paymentDayOfMonth,
        String documentUrl,
        LocalDateTime signedAt,
        List<RoommateResponse> roommates
) {
    public MyContractResponse(
            Long id,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal rentPrice,
            BigDecimal deposit,
            String terms
    ) {
        this(id, status, startDate, endDate, rentPrice, deposit, terms,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, List.of());
    }

    public static MyContractResponse empty() {
        return new MyContractResponse(null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, List.of());
    }
}
