package com.roomcare.backend.dto;

import com.roomcare.backend.entity.AuthVerificationToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotNull(message = "Phương thức khôi phục không được để trống")
        AuthVerificationToken.Channel channel,

        @NotBlank(message = "Email hoặc số điện thoại không được để trống")
        @Size(max = 100, message = "Thông tin liên hệ tối đa 100 ký tự")
        String target
) {}
