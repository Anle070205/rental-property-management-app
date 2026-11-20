package com.roomcare.backend.dto;

public record VerifyResetOtpResponse(
        String email,
        String resetToken,
        int expiresInMinutes
) {}
