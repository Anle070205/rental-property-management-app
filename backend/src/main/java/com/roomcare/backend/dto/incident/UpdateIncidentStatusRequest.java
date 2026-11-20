package com.roomcare.backend.dto.incident;

import com.roomcare.backend.entity.Incident;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateIncidentStatusRequest(
        @NotNull(message = "Vui lòng chọn trạng thái xử lý")
        Incident.Status status,

        @Size(max = 1000, message = "Phản hồi của chủ trọ tối đa 1000 ký tự")
        String landlordResponse
) {}
