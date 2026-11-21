package com.roomcare.backend.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionTenantAccountResponse {
    private Long tenantId;
    private Long userId;
    private String accountStatus; // NONE, PENDING_ACTIVATION, ACTIVE
    private String action; // CREATED_PENDING_ACTIVATION, LINK_OTP_SENT, ALREADY_ACTIVE
    private String channel;
    private String target;
    private Integer resendCooldownSeconds;
    private String message;
}
