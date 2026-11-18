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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateInvoiceRequest {

    private Long propertyId;

    private List<Long> roomIds;

    @NotBlank(message = "Kỳ hóa đơn không được để trống")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Kỳ hóa đơn phải có định dạng YYYY-MM")
    private String billingMonth;

    @NotNull(message = "Hạn thanh toán không được để trống")
    private LocalDate dueDate;

    @DecimalMin(value = "0.0", message = "Phí internet mặc định phải >= 0")
    private BigDecimal defaultInternetFee = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Phí dịch vụ mặc định phải >= 0")
    private BigDecimal defaultServiceFee = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Khoản phát sinh phải >= 0")
    private BigDecimal otherFees = BigDecimal.ZERO;

    private Boolean onlyOccupiedRooms = Boolean.TRUE;

    private String notes;
}
