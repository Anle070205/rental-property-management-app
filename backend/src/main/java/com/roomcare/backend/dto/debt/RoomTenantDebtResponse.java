package com.roomcare.backend.dto.debt;

import com.roomcare.backend.dto.invoice.InvoiceResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTenantDebtResponse {
    private Long roomId;
    private String roomCode;
    private Long propertyId;
    private String propertyName;
    private Long tenantId;
    private String tenantName;
    private String tenantPhone;
    private BigDecimal totalBilled;
    private BigDecimal totalPaid;
    private BigDecimal totalDebt;
    private int unpaidInvoicesCount;
    private int overdueInvoicesCount;
    private long maxDaysOverdue;
    private String overdueLevel; // NOT_DUE, OVERDUE_1_7, OVERDUE_8_30, OVERDUE_30_PLUS
    private Long latestInvoiceId;
    private List<InvoiceResponse> unpaidInvoices = new ArrayList<>();
    private List<DebtHistoryItemResponse> debtHistory = new ArrayList<>();
}
