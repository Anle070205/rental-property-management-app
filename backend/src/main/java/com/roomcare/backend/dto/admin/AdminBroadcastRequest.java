package com.roomcare.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminBroadcastRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @NotBlank(message = "Nội dung không được để trống")
        @Size(max = 4000, message = "Nội dung tối đa 4000 ký tự")
        String content,

        @NotBlank(message = "Đối tượng nhận không được để trống")
        String targetAudience
) {}
