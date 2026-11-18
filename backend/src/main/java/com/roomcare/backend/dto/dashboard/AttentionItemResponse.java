package com.roomcare.backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttentionItemResponse {
    private Long roomId;
    private String roomCode;
    private String propertyName;
    private String tenantName;
    private String category; // EXPIRING_SOON | INCIDENT | OVERDUE_DEBT | LOW_HEALTH_SCORE
    private String reason;
    private int healthScore;
    private BigDecimal unpaidDebt;
}
