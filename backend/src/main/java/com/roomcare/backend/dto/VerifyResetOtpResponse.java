package com.roomcare.backend.dto;

public record VerifyResetOtpResponse(
        String channel,
        String target,
        String resetToken,
        int expiresInMinutes
) {}
