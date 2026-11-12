package com.roomcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String emailOrPhone;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
