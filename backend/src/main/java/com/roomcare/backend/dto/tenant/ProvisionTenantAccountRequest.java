package com.roomcare.backend.dto.tenant;

import com.roomcare.backend.entity.AuthVerificationToken;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionTenantAccountRequest {

    @NotNull(message = "Phương thức gửi mã kích hoạt không được để trống")
    private AuthVerificationToken.Channel channel;

    private String target;

    private Boolean confirmLinkExisting = Boolean.TRUE;
}
