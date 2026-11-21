package com.roomcare.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "contracts",
    indexes = {
        @Index(name = "idx_contract_landlord_status", columnList = "landlord_id, status"),
        @Index(name = "idx_contract_room_status", columnList = "room_id, status"),
        @Index(name = "idx_contract_tenant_status", columnList = "tenant_id, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_code", nullable = false, unique = true, length = 50)
    private String contractCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "monthly_rent", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "electricity_unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal electricityUnitPrice = new BigDecimal("3500");

    @Column(name = "water_unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal waterUnitPrice = new BigDecimal("15000");

    @Enumerated(EnumType.STRING)
    @Column(name = "water_billing_mode", nullable = false, length = 20)
    private WaterBillingMode waterBillingMode = WaterBillingMode.METERED;

    @Column(name = "internet_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal internetFee = BigDecimal.ZERO;

    @Column(name = "garbage_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal garbageFee = BigDecimal.ZERO;

    @Column(name = "service_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(name = "payment_day_of_month", nullable = false)
    private Integer paymentDayOfMonth = 5;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
