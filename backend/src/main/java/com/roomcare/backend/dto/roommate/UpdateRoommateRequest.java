package com.roomcare.backend.dto.roommate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoommateRequest {

    @NotBlank(message = "Họ và tên người ở cùng không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^(|0[0-9]{9,10}|\\+84[0-9]{9})$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Pattern(regexp = "^(|[0-9]{9,12})$", message = "Số CMND/CCCD không hợp lệ (9-12 chữ số)")
    private String identityNumber;

    private LocalDate dateOfBirth;

    @Size(max = 60, message = "Quan hệ tối đa 60 ký tự")
    private String relationship;

    private LocalDate moveInDate;

    private Boolean isRepresentative;

    private Boolean hasPortalAccess;

    private Boolean consentConfirmed;

    private String notes;
}
