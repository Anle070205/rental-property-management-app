package com.roomcare.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register Request DTO
 * CHỈ CHO LANDLORD - Tenant account được tạo bởi Landlord
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải có 10 số và bắt đầu bằng 0")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6-50 ký tự")
    private String password;

    // Role sẽ luôn là LANDLORD cho register API
    // TENANT account được tạo bởi Landlord thông qua Contract creation
    private String role = "LANDLORD"; // Default và fixed value
}

