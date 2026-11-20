package com.roomcare.backend.dto.contract;

import com.roomcare.backend.entity.WaterBillingMode;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContractRequest {

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    @NotNull(message = "Tiền thuê hằng tháng không được để trống")
    @DecimalMin(value = "0.0", message = "Tiền thuê phải lớn hơn hoặc bằng 0")
    private BigDecimal monthlyRent;

    @DecimalMin(value = "0.0", message = "Tiền cọc phải lớn hơn hoặc bằng 0")
    private BigDecimal depositAmount;

    @DecimalMin(value = "0.0", message = "Đơn giá điện phải lớn hơn hoặc bằng 0")
    private BigDecimal electricityUnitPrice;

    @DecimalMin(value = "0.0", message = "Đơn giá nước phải lớn hơn hoặc bằng 0")
    private BigDecimal waterUnitPrice;

    private WaterBillingMode waterBillingMode;

    @DecimalMin(value = "0.0", message = "Phí internet phải lớn hơn hoặc bằng 0")
    private BigDecimal internetFee;

    @DecimalMin(value = "0.0", message = "Phí rác phải lớn hơn hoặc bằng 0")
    private BigDecimal garbageFee;

    @DecimalMin(value = "0.0", message = "Phí dịch vụ phải lớn hơn hoặc bằng 0")
    private BigDecimal serviceFee;

    @Min(value = 1, message = "Ngày thanh toán phải từ 1 đến 28")
    @Max(value = 28, message = "Ngày thanh toán phải từ 1 đến 28")
    private Integer paymentDayOfMonth = 5;

    private String terms;

    @Size(max = 500, message = "Đường dẫn tài liệu tối đa 500 ký tự")
    private String documentUrl;
}
