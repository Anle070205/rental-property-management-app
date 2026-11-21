package com.roomcare.backend.dto;

import com.roomcare.backend.entity.AuthVerificationToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActivateAccountRequest(
        @NotNull(message = "Phương thức kích hoạt không được để trống")
        AuthVerificationToken.Channel channel,

        @NotBlank(message = "Email hoặc số điện thoại không được để trống")
        String target,

        @NotBlank(message = "Mã OTP không được để trống")
        @Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm đúng 6 chữ số")
        String otp,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 6, max = 50, message = "Mật khẩu mới phải từ 6-50 ký tự")
        String newPassword,

        @NotBlank(message = "Xác nhận mật khẩu mới không được để trống")
        String confirmPassword
) {}
