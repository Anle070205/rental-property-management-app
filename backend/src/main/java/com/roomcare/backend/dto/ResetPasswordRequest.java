package com.roomcare.backend.dto;

import com.roomcare.backend.entity.AuthVerificationToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotNull(message = "Phương thức khôi phục không được để trống")
        AuthVerificationToken.Channel channel,

        @NotBlank(message = "Email hoặc số điện thoại không được để trống")
        String target,

        @NotBlank(message = "Phiên đặt lại mật khẩu không hợp lệ")
        String resetToken,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 6, max = 50, message = "Mật khẩu mới phải từ 6-50 ký tự")
        String newPassword,

        @NotBlank(message = "Xác nhận mật khẩu mới không được để trống")
        String confirmPassword
) {}
