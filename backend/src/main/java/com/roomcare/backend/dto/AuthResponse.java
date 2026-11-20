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
    private Boolean emailVerified = true;
    private Boolean requiresEmailVerification = false;
    private Integer resendCooldownSeconds;

    public AuthResponse(String accessToken, Long userId, String fullName, String email, String role) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.emailVerified = true;
        this.requiresEmailVerification = false;
    }

    public static AuthResponse pendingVerification(
            Long userId,
            String fullName,
            String email,
            String role,
            int resendCooldownSeconds
    ) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(null);
        response.setTokenType(null);
        response.setUserId(userId);
        response.setFullName(fullName);
        response.setEmail(email);
        response.setRole(role);
        response.setEmailVerified(false);
        response.setRequiresEmailVerification(true);
        response.setResendCooldownSeconds(resendCooldownSeconds);
        return response;
    }
}
