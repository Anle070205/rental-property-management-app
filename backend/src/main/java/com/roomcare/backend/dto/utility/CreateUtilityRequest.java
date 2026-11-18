package com.roomcare.backend.dto.utility;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUtilityRequest {

    @NotNull(message = "Phòng không được để trống")
    private Long roomId;

    @NotBlank(message = "Kỳ ghi nhận không được để trống")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Kỳ ghi nhận phải có định dạng YYYY-MM")
    private String billingMonth;

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
