package com.roomcare.backend.dto.user;

import com.roomcare.backend.entity.Role;

public record ProfileResponse(String fullName, String email, String phone, Role role) {}
