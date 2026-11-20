package com.roomcare.backend.dto;

import com.roomcare.backend.entity.AuthVerificationToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResendOtpRequest(
        @NotNull(message = "Phương thức xác thực không được để trống")
        AuthVerificationToken.Channel channel,

        @NotBlank(message = "Email hoặc số điện thoại không được để trống")
        String target
) {}
