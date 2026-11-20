package com.roomcare.backend.dto.booking;

import com.roomcare.backend.entity.BookingRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespondBookingRequest {

    @NotNull(message = "Trạng thái phản hồi không được để trống")
    private BookingRequest.Status status;

    @Size(max = 1000, message = "Ghi chú chủ nhà tối đa 1000 ký tự")
    private String landlordNote;
}
