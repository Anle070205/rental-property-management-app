package com.roomcare.backend.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateAccountRequest(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phone,

        @NotBlank(message = "Vai trò không được để trống")
        String role,

        Boolean isActive,

        @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6-50 ký tự")
        String password,

        Long landlordId,
        Long propertyId,
        Long roomId
) {}
