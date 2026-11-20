package com.roomcare.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự") String phone
) {}
