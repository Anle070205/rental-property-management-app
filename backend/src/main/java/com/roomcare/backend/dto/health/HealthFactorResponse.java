package com.roomcare.backend.dto.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthFactorResponse {
    private String factorName;
    private int score;
    private int maxScore;
    private int weightPercent;
    private String status; // GOOD | WARNING | RISK
    private String detail;
}
