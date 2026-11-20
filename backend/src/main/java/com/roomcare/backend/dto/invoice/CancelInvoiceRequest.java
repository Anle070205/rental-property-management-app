package com.roomcare.backend.dto.invoice;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelInvoiceRequest {

    @Size(max = 1000, message = "Lý do hủy hóa đơn không vượt quá 1000 ký tự")
    private String reason;
}
