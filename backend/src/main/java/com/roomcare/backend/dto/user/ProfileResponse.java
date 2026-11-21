package com.roomcare.backend.dto.user;

import com.roomcare.backend.entity.Role;

public record ProfileResponse(
        String fullName,
        String email,
        String phone,
        Role role,
        boolean emailVerified,
        boolean phoneVerified
) {
    public ProfileResponse(String fullName, String email, String phone, Role role) {
        this(fullName, email, phone, role, email != null && !email.isBlank(), phone != null && !phone.isBlank());
    }
}
