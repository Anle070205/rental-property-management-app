package com.roomcare.backend.dto.tenant;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {
    
    @NotNull(message = "ID nhà trọ không được để trống")
    private Long propertyId;
    
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    private String fullName;
    
    @Pattern(regexp = "^(|0[0-9]{9,10}|\\+84[0-9]{9})$", message = "Số điện thoại không hợp lệ (ví dụ: 0912345678 hoặc +84912345678)")
    private String phone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @NotBlank(message = "Số CMND/CCCD không được để trống")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "Số CMND/CCCD không hợp lệ")
    private String identityNumber;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Size(max = 100, message = "Liên hệ khẩn cấp không được vượt quá 100 ký tự")
    private String emergencyContact;

    private LocalDate moveInDate;

    private Boolean consentConfirmed = Boolean.TRUE;
}
