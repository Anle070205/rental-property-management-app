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
    name = "booking_requests",
    indexes = {
        @Index(name = "idx_booking_landlord_created", columnList = "landlord_id,created_at"),
        @Index(name = "idx_booking_tenant_created", columnList = "tenant_user_id,created_at"),
        @Index(name = "idx_booking_room_status", columnList = "room_id,status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    public enum RequestType {
        BOOKING,
        CONTACT
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_user_id", nullable = false)
    private User tenantUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 20)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "tenant_name", nullable = false, length = 100)
    private String tenantName;

    @Column(name = "tenant_phone", length = 20)
    private String tenantPhone;

    @Column(name = "tenant_email", length = 100)
    private String tenantEmail;

    @Column(name = "identity_number", length = 20)
    private String identityNumber;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "landlord_note", columnDefinition = "TEXT")
    private String landlordNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
