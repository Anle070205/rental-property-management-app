package com.roomcare.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "guest_stays",
        indexes = {
                @Index(name = "idx_guest_stays_tenant_created", columnList = "tenant_id,created_at"),
                @Index(name = "idx_guest_stays_landlord_created", columnList = "landlord_id,created_at")
        }
)
@Data
public class GuestStay {

    public enum Relationship {
        FRIEND,
        RELATIVE,
        OTHER
    }

    public enum Status {
        PENDING,
        RECEIVED,
        NEEDS_INFO,
        COMPLETED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @Column(name = "guest_full_name", nullable = false, length = 120)
    private String guestFullName;

    @Column(name = "guest_identity_number", nullable = false, length = 20)
    private String guestIdentityNumber;

    @Column(name = "guest_date_of_birth")
    private LocalDate guestDateOfBirth;

    @Column(name = "guest_gender", length = 20)
    private String guestGender;

    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relationship relationship = Relationship.FRIEND;

    @Column(name = "stay_start_at", nullable = false)
    private LocalDateTime stayStartAt;

    @Column(name = "stay_end_at", nullable = false)
    private LocalDateTime stayEndAt;

    @Column(length = 1000)
    private String note;

    @Column(name = "consent_confirmed", nullable = false)
    private Boolean consentConfirmed = false;

    @Column(name = "qr_verified", nullable = false)
    private Boolean qrVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "landlord_note", length = 500)
    private String landlordNote;

    @Column(name = "retention_until", nullable = false)
    private LocalDateTime retentionUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
