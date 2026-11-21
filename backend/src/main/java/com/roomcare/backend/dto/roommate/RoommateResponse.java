package com.roomcare.backend.dto.roommate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoommateResponse {
    private Long id;
    private Long tenantId;
    private Long roomId;
    private String roomCode;
    private Long contractId;
    private String fullName;
    private String phone;
    private String maskedIdentityNumber;
    private LocalDate dateOfBirth;
    private String relationship;
    private LocalDate moveInDate;
    private Boolean isRepresentative;
    private Boolean hasPortalAccess;
    private Boolean consentConfirmed;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
