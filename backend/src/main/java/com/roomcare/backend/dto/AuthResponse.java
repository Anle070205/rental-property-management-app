package com.roomcare.backend.dto;

import com.roomcare.backend.entity.AuthVerificationToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication Response DTO - Hỗ trợ đăng nhập (có JWT) và phản hồi chờ xác thực OTP (accessToken = null)
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
    private String phone;
    private String role;
    private Boolean emailVerified = true;
    private Boolean phoneVerified = true;
    private Boolean requiresVerification = false;
    private String verificationChannel;
    private String verificationTarget;
    private Integer resendCooldownSeconds;

    public AuthResponse(String accessToken, Long userId, String fullName, String email, String role) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public static AuthResponse verifiedLogin(
            String accessToken,
            Long userId,
            String fullName,
            String email,
            String phone,
            String role,
            boolean emailVerified,
            boolean phoneVerified
    ) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");
        response.setUserId(userId);
        response.setFullName(fullName);
        response.setEmail(email);
        response.setPhone(phone);
        response.setRole(role);
        response.setEmailVerified(emailVerified);
        response.setPhoneVerified(phoneVerified);
        response.setRequiresVerification(false);
        return response;
    }

    public static AuthResponse pendingVerification(
            Long userId,
            String fullName,
            String email,
            String phone,
            String role,
            AuthVerificationToken.Channel channel,
            String target,
            int resendCooldownSeconds
    ) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(null);
        response.setTokenType(null);
        response.setUserId(userId);
        response.setFullName(fullName);
        response.setEmail(email);
        response.setPhone(phone);
        response.setRole(role);
        response.setEmailVerified(false);
        response.setPhoneVerified(false);
        response.setRequiresVerification(true);
        response.setVerificationChannel(channel.name());
        response.setVerificationTarget(target);
        response.setResendCooldownSeconds(resendCooldownSeconds);
        return response;
    }
}
