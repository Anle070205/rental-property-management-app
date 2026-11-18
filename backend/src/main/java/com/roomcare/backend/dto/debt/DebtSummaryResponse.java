package com.roomcare.backend.dto.debt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtSummaryResponse {
    private BigDecimal totalDebtAmount;
    private int totalUnpaidInvoices;
    private int totalOverdueInvoices;
    private int roomsWithDebtCount;
    private List<RoomTenantDebtResponse> items = new ArrayList<>();
}
