package com.roomcare.backend.dto.tenant;

import jakarta.validation.constraints.Size;

public record SubmitPaymentProofRequest(
        @Size(max = 1000, message = "Ghi chú minh chứng không vượt quá 1000 ký tự")
        String note,
        @Size(max = 500, message = "Đường dẫn ảnh minh chứng không vượt quá 500 ký tự")
        String proofUrl
) {}
