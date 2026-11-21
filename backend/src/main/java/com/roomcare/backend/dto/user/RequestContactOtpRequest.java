package com.roomcare.backend.dto.user;

import com.roomcare.backend.entity.AuthVerificationToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestContactOtpRequest(
        @NotNull(message = "Phương thức liên hệ không được để trống")
        AuthVerificationToken.Channel channel,

        @NotBlank(message = "Email hoặc số điện thoại không được để trống")
        String target
) {}
