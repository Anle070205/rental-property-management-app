package com.roomcare.backend.dto.utility;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviousUtilityIndexResponse {
    private Long roomId;
    private String previousBillingMonth;
    private BigDecimal suggestedPrevElectricIndex;
    private BigDecimal suggestedPrevWaterIndex;
    private BigDecimal lastElectricUnitPrice;
    private BigDecimal lastWaterUnitPrice;
    private BigDecimal lastInternetFee;
    private BigDecimal lastServiceFee;
}
