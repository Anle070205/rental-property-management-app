package com.roomcare.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Register Request DTO - Hỗ trợ chọn 1 trong 2 phương thức: EMAIL (Gmail) hoặc PHONE (Số điện thoại VN)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
    private String fullName;

    @Pattern(
            regexp = "^(?i)(EMAIL|PHONE)$",
            message = "Phương thức xác thực chỉ hỗ trợ EMAIL hoặc PHONE"
    )
    private String verificationMethod = "EMAIL";

    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6-50 ký tự")
    private String password;

    private String role = "LANDLORD";

    public RegisterRequest(String fullName, String email, String phone, String password, String role) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.verificationMethod = (email != null && !email.isBlank()) ? "EMAIL" : "PHONE";
    }
}

