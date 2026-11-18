package com.roomcare.backend.dto.incident;

import com.roomcare.backend.entity.Incident;
import jakarta.validation.constraints.*;

public record CreateIncidentRequest(
        @NotNull(message = "Vui lòng chọn loại sự cố") Incident.Type type,
        @NotBlank(message = "Vui lòng nhập tiêu đề")
        @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự") String title,
        @NotBlank(message = "Vui lòng mô tả sự cố")
        @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự") String description,
        @NotNull(message = "Vui lòng chọn mức ưu tiên") Incident.Priority priority,
        @Size(max = 1024, message = "Liên kết ảnh tối đa 1024 ký tự")
        @Pattern(regexp = "https?://\\S+", message = "Liên kết ảnh phải dùng HTTP hoặc HTTPS") String imageUrl) {}
