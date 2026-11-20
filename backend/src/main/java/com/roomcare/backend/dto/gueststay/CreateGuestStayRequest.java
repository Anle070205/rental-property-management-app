package com.roomcare.backend.dto.gueststay;

import com.roomcare.backend.entity.GuestStay;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateGuestStayRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên khách lưu trú")
        @Size(max = 120, message = "Họ và tên khách tối đa 120 ký tự")
        String guestFullName,

        @NotBlank(message = "Vui lòng nhập số CCCD/CMND của khách")
        @Pattern(regexp = "^(\\d{9}|\\d{12})$", message = "Số định danh phải gồm 9 hoặc 12 chữ số")
        String guestIdentityNumber,

        LocalDate guestDateOfBirth,

        @Size(max = 20, message = "Giới tính tối đa 20 ký tự")
        String guestGender,

        @Size(max = 20, message = "Số điện thoại liên hệ tối đa 20 ký tự")
        String guestPhone,

        @NotNull(message = "Vui lòng chọn quan hệ với người thuê")
        GuestStay.Relationship relationship,

        @NotNull(message = "Vui lòng chọn thời gian bắt đầu lưu trú")
        LocalDateTime stayStartAt,

        @NotNull(message = "Vui lòng chọn thời gian kết thúc dự kiến")
        LocalDateTime stayEndAt,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String note,

        @AssertTrue(message = "Cần xác nhận khách đồng ý cung cấp thông tin CCCD để khai báo lưu trú")
        boolean consentConfirmed,

        boolean qrVerified
) {}
