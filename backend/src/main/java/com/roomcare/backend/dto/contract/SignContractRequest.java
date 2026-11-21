package com.roomcare.backend.dto.contract;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignContractRequest {

    @Size(max = 500, message = "Đường dẫn tài liệu tối đa 500 ký tự")
    private String documentUrl;

    private String note;
}
