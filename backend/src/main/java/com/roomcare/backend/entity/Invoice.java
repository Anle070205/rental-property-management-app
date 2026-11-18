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
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "billing_month"})
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

    @Column(name = "utility_record_id")
    private Long utilityRecordId;

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

    @Column(name = "service_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceAmount = BigDecimal.ZERO;

    @Column(name = "other_fees", nullable = false, precision = 15, scale = 2)
    private BigDecimal otherFees = BigDecimal.ZERO;

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
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
