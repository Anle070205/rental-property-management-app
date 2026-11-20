package com.roomcare.backend.dto.booking;

import com.roomcare.backend.entity.BookingRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "Vui lòng chọn phòng")
    private Long roomId;

    @NotNull(message = "Loại yêu cầu không được để trống")
    private BookingRequest.RequestType requestType;

    private LocalDate moveInDate;

    @Size(max = 20, message = "CMND/CCCD tối đa 20 ký tự")
    private String identityNumber;

    @Size(max = 1000, message = "Lời nhắn tối đa 1000 ký tự")
    private String message;
}
