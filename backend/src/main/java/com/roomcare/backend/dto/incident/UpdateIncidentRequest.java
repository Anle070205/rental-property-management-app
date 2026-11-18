package com.roomcare.backend.dto.incident;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateIncidentRequest(
        @Size(max = 4000, message = "Mô tả bổ sung tối đa 4000 ký tự") String description,
        @Size(max = 1024, message = "Liên kết ảnh tối đa 1024 ký tự")
        @Pattern(regexp = "https?://\\S+", message = "Liên kết ảnh phải dùng HTTP hoặc HTTPS") String imageUrl) {}
