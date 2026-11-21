package com.roomcare.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "roommates",
    indexes = {
        @Index(name = "idx_roommate_tenant", columnList = "tenant_id"),
        @Index(name = "idx_roommate_room", columnList = "room_id"),
        @Index(name = "idx_roommate_contract", columnList = "contract_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Roommate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "identity_number", length = 20)
    private String identityNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 60)
    private String relationship;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Column(name = "is_representative", nullable = false)
    private Boolean isRepresentative = Boolean.FALSE;

    @Column(name = "has_portal_access", nullable = false)
    private Boolean hasPortalAccess = Boolean.FALSE;

    @Column(name = "consent_confirmed", nullable = false)
    private Boolean consentConfirmed = Boolean.FALSE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
