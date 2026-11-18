package com.roomcare.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "\\+?[0-9]{8,14}|[0-9]{15}", message = "Số điện thoại không hợp lệ") String phone) {}
