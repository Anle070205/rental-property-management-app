package com.roomcare.backend.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateAccountRequest(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phone,

        String role,
        Boolean isActive
) {}
