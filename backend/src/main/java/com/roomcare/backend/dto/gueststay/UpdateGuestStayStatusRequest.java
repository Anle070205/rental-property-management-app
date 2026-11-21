package com.roomcare.backend.dto.gueststay;

import com.roomcare.backend.entity.GuestStay;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateGuestStayStatusRequest(
        @NotNull(message = "Vui lòng chọn trạng thái xử lý")
        GuestStay.Status status,

        @Size(max = 500, message = "Ghi chú của chủ trọ tối đa 500 ký tự")
        String landlordNote
) {}
