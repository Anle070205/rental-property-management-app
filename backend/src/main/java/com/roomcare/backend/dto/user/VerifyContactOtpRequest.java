package com.roomcare.backend.dto.user;

import com.roomcare.backend.entity.AuthVerificationToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerifyContactOtpRequest(
        @NotNull(message = "Phương thức liên hệ không được để trống")
        AuthVerificationToken.Channel channel,

        @NotBlank(message = "Email hoặc số điện thoại không được để trống")
        String target,

        @NotBlank(message = "Mã OTP không được để trống")
        @Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm đúng 6 chữ số")
        String otp
) {}
