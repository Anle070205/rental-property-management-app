package com.roomcare.backend.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    private Long id;
    private Long userId; // Nullable
    private Long landlordId;
    private Long propertyId;
    private String propertyName;
    private String fullName;
    private String phone;
    private String email;
    private String identityNumber; // Sẽ được mask trong list view
    private LocalDate dateOfBirth;
    private String address;
    private String emergencyContact;
    private String currentRoom; // TODO: Phase 3 - phòng đang thuê
    private String currentContract; // TODO: Phase 3 - hợp đồng hiện tại
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Helper method để mask số CMND/CCCD
    public void maskIdentityNumber() {
        if (identityNumber != null && identityNumber.length() > 4) {
            int visibleChars = 4;
            String masked = "*".repeat(identityNumber.length() - visibleChars);
            this.identityNumber = masked + identityNumber.substring(identityNumber.length() - visibleChars);
        }
    }
}
