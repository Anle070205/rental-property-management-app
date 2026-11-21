package com.roomcare.backend.dto.admin;

public record AdminChangePasswordResponse(
        String accessToken,
        String message
) {}
