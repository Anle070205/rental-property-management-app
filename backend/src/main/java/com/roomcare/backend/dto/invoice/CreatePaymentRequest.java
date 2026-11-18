package com.roomcare.backend.dto.invoice;

import com.roomcare.backend.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "1.0", message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    private String transactionCode;

    private String receiptNote;

    private Boolean confirmedByLandlord = Boolean.TRUE;
}
