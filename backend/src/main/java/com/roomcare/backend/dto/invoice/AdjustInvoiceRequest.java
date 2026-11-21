package com.roomcare.backend.dto.invoice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdjustInvoiceRequest {

    /** Khoản điều chỉnh cộng/trừ thêm (+/- VND) */
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @NotBlank(message = "Lý do điều chỉnh không được để trống")
    @Size(max = 1000, message = "Lý do điều chỉnh không vượt quá 1000 ký tự")
    private String adjustmentReason;

    @DecimalMin(value = "0.0", message = "Phí rác phải >= 0")
    private BigDecimal garbageFee;

    @DecimalMin(value = "0.0", message = "Khoản phát sinh phải >= 0")
    private BigDecimal otherFees;

    @DecimalMin(value = "0.0", message = "Giảm trừ phải >= 0")
    private BigDecimal discountAmount;

    private LocalDate dueDate;

    @Size(max = 2000, message = "Ghi chú không vượt quá 2000 ký tự")
    private String notes;
}
