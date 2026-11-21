package com.roomcare.backend.dto.gueststay;

import com.roomcare.backend.entity.GuestStay;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GuestStayResponse(
        Long id,
        Long tenantId,
        String tenantName,
        Long roomId,
        String roomCode,
        String propertyName,
        String guestFullName,
        String maskedIdentityNumber,
        LocalDate guestDateOfBirth,
        String guestGender,
        String guestPhone,
        GuestStay.Relationship relationship,
        LocalDateTime stayStartAt,
        LocalDateTime stayEndAt,
        String note,
        boolean consentConfirmed,
        boolean qrVerified,
        GuestStay.Status status,
        String landlordNote,
        LocalDateTime retentionUntil,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String legalDisclaimer
) {
    private static final String DISCLAIMER =
            "Thông báo nội bộ RoomCare gửi chủ nhà trọ; không thay thế thủ tục đăng ký tạm trú với cơ quan chức năng.";

    public static GuestStayResponse from(GuestStay entity) {
        return new GuestStayResponse(
                entity.getId(),
                entity.getTenant() != null ? entity.getTenant().getId() : null,
                entity.getTenant() != null ? entity.getTenant().getFullName() : null,
                entity.getRoom() != null ? entity.getRoom().getId() : null,
                entity.getRoom() != null ? entity.getRoom().getRoomCode() : null,
                entity.getRoom() != null && entity.getRoom().getProperty() != null
                        ? entity.getRoom().getProperty().getName()
                        : (entity.getTenant() != null && entity.getTenant().getProperty() != null
                                ? entity.getTenant().getProperty().getName()
                                : null),
                entity.getGuestFullName(),
                maskIdentityNumber(entity.getGuestIdentityNumber()),
                entity.getGuestDateOfBirth(),
                entity.getGuestGender(),
                entity.getGuestPhone(),
                entity.getRelationship(),
                entity.getStayStartAt(),
                entity.getStayEndAt(),
                entity.getNote(),
                Boolean.TRUE.equals(entity.getConsentConfirmed()),
                Boolean.TRUE.equals(entity.getQrVerified()),
                entity.getStatus(),
                entity.getLandlordNote(),
                entity.getRetentionUntil(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                DISCLAIMER
        );
    }

    public static String maskIdentityNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String clean = raw.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        if (clean.length() <= 7) {
            return "*".repeat(clean.length() - 3) + clean.substring(clean.length() - 3);
        }
        return clean.substring(0, 3) + "*".repeat(clean.length() - 7) + clean.substring(clean.length() - 4);
    }
}
