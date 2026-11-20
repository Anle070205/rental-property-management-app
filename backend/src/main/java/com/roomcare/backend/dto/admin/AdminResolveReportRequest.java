package com.roomcare.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResolveReportRequest(
        @NotBlank(message = "Nội dung phản hồi không được để trống")
        @Size(max = 2000, message = "Nội dung phản hồi tối đa 2000 ký tự")
        String responseNote,

        @NotBlank(message = "Trạng thái sau xử lý không được để trống")
        String status
) {}
