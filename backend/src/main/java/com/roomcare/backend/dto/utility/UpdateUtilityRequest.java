package com.roomcare.backend.dto.utility;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUtilityRequest {

    @NotNull(message = "Chỉ số điện cũ không được để trống")
    @DecimalMin(value = "0.0", message = "Chỉ số điện cũ phải >= 0")
    private BigDecimal prevElectricIndex;

    @NotNull(message = "Chỉ số điện mới không được để trống")
    @DecimalMin(value = "0.0", message = "Chỉ số điện mới phải >= 0")
    private BigDecimal currElectricIndex;

    @NotNull(message = "Đơn giá điện không được để trống")
    @DecimalMin(value = "0.0", message = "Đơn giá điện phải >= 0")
    private BigDecimal electricUnitPrice;

    @NotNull(message = "Chỉ số nước cũ không được để trống")
    @DecimalMin(value = "0.0", message = "Chỉ số nước cũ phải >= 0")
    private BigDecimal prevWaterIndex;

    @NotNull(message = "Chỉ số nước mới không được để trống")
    @DecimalMin(value = "0.0", message = "Chỉ số nước mới phải >= 0")
    private BigDecimal currWaterIndex;

    @NotNull(message = "Đơn giá nước không được để trống")
    @DecimalMin(value = "0.0", message = "Đơn giá nước phải >= 0")
    private BigDecimal waterUnitPrice;

    @DecimalMin(value = "0.0", message = "Phí internet phải >= 0")
    private BigDecimal internetFee = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Phí dịch vụ phải >= 0")
    private BigDecimal serviceFee = BigDecimal.ZERO;

    private Boolean isAdjustment = Boolean.FALSE;

    private String adjustmentReason;

    private String notes;
}
