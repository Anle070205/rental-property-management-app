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
    name = "invoices",
    indexes = {
        @Index(name = "idx_invoice_room_month", columnList = "room_id, billing_month")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_code", nullable = false, unique = true, length = 40)
    private String invoiceCode;

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
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(name = "utility_record_id")
    private Long utilityRecordId;

    @Column(name = "prev_electric_index", precision = 12, scale = 2)
    private BigDecimal prevElectricIndex;

    @Column(name = "curr_electric_index", precision = 12, scale = 2)
    private BigDecimal currElectricIndex;

    @Column(name = "electric_usage", precision = 12, scale = 2)
    private BigDecimal electricUsage;

    @Column(name = "electric_unit_price", precision = 15, scale = 2)
    private BigDecimal electricUnitPrice;

    @Column(name = "prev_water_index", precision = 12, scale = 2)
    private BigDecimal prevWaterIndex;

    @Column(name = "curr_water_index", precision = 12, scale = 2)
    private BigDecimal currWaterIndex;

    @Column(name = "water_usage", precision = 12, scale = 2)
    private BigDecimal waterUsage;

    @Column(name = "water_unit_price", precision = 15, scale = 2)
    private BigDecimal waterUnitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "water_billing_mode", nullable = false, length = 20)
    private WaterBillingMode waterBillingMode = WaterBillingMode.METERED;

    @Column(name = "occupant_count", nullable = false)
    private Integer occupantCount = 1;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth; // YYYY-MM

    @Column(name = "room_rent", nullable = false, precision = 15, scale = 2)
    private BigDecimal roomRent = BigDecimal.ZERO;

    @Column(name = "electricity_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal electricityAmount = BigDecimal.ZERO;

    @Column(name = "water_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal waterAmount = BigDecimal.ZERO;

    @Column(name = "internet_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal internetAmount = BigDecimal.ZERO;

    @Column(name = "garbage_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal garbageFee = BigDecimal.ZERO;

    @Column(name = "service_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceAmount = BigDecimal.ZERO;

    @Column(name = "other_fees", nullable = false, precision = 15, scale = 2)
    private BigDecimal otherFees = BigDecimal.ZERO;

    @Column(name = "adjustment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @Column(name = "adjustment_reason", columnDefinition = "TEXT")
    private String adjustmentReason;

    @Column(name = "adjustment_history", columnDefinition = "TEXT")
    private String adjustmentHistory;

    @Column(name = "previous_debt", nullable = false, precision = 15, scale = 2)
    private BigDecimal previousDebt = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "payment_proof_note", columnDefinition = "TEXT")
    private String paymentProofNote;

    @Column(name = "payment_proof_url", length = 500)
    private String paymentProofUrl;

    @Column(name = "payment_proof_submitted_at")
    private LocalDateTime paymentProofSubmittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
