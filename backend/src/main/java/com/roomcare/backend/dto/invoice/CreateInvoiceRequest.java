package com.roomcare.backend.dto.invoice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {

    @NotNull(message = "Phòng không được để trống")
    private Long roomId;

    private Long tenantId;

    @NotBlank(message = "Kỳ hóa đơn không được để trống")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Kỳ hóa đơn phải có định dạng YYYY-MM")
    private String billingMonth;

    @NotNull(message = "Hạn thanh toán không được để trống")
    private LocalDate dueDate;

    @DecimalMin(value = "0.0", message = "Tiền phòng phải lớn hơn hoặc bằng 0")
    private BigDecimal roomRent;

    @DecimalMin(value = "0.0", message = "Tiền điện phải lớn hơn hoặc bằng 0")
    private BigDecimal electricityAmount;

    @DecimalMin(value = "0.0", message = "Tiền nước phải lớn hơn hoặc bằng 0")
    private BigDecimal waterAmount;

    @DecimalMin(value = "0.0", message = "Tiền internet phải lớn hơn hoặc bằng 0")
    private BigDecimal internetAmount;

    @DecimalMin(value = "0.0", message = "Phí dịch vụ phải lớn hơn hoặc bằng 0")
    private BigDecimal serviceAmount;

    @DecimalMin(value = "0.0", message = "Khoản phát sinh phải lớn hơn hoặc bằng 0")
    private BigDecimal otherFees = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Nợ kỳ trước phải lớn hơn hoặc bằng 0")
    private BigDecimal previousDebt;

    @DecimalMin(value = "0.0", message = "Giảm trừ phải lớn hơn hoặc bằng 0")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private Boolean useUtilityRecord = Boolean.TRUE;

    private String notes;
}
