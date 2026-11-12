package com.roomcare.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String fullName;
    private String email;
    private String role;

    public AuthResponse(String accessToken, Long userId, String fullName, String email, String role) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }
}
