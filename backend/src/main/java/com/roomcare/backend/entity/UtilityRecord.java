package com.roomcare.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "utility_records",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "billing_month"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth; // YYYY-MM

    @Column(name = "prev_electric_index", nullable = false, precision = 12, scale = 2)
    private BigDecimal prevElectricIndex = BigDecimal.ZERO;

    @Column(name = "curr_electric_index", nullable = false, precision = 12, scale = 2)
    private BigDecimal currElectricIndex = BigDecimal.ZERO;

    @Column(name = "electric_usage", nullable = false, precision = 12, scale = 2)
    private BigDecimal electricUsage = BigDecimal.ZERO;

    @Column(name = "electric_unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal electricUnitPrice = BigDecimal.ZERO;

    @Column(name = "electricity_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal electricityAmount = BigDecimal.ZERO;

    @Column(name = "prev_water_index", nullable = false, precision = 12, scale = 2)
    private BigDecimal prevWaterIndex = BigDecimal.ZERO;

    @Column(name = "curr_water_index", nullable = false, precision = 12, scale = 2)
    private BigDecimal currWaterIndex = BigDecimal.ZERO;

    @Column(name = "water_usage", nullable = false, precision = 12, scale = 2)
    private BigDecimal waterUsage = BigDecimal.ZERO;

    @Column(name = "water_unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal waterUnitPrice = BigDecimal.ZERO;

    @Column(name = "water_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal waterAmount = BigDecimal.ZERO;

    @Column(name = "internet_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal internetFee = BigDecimal.ZERO;

    @Column(name = "service_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(name = "total_utility_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalUtilityAmount = BigDecimal.ZERO;

    @Column(name = "is_adjustment", nullable = false)
    private Boolean isAdjustment = Boolean.FALSE;

    @Column(name = "adjustment_reason", columnDefinition = "TEXT")
    private String adjustmentReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "history_log", columnDefinition = "TEXT")
    private String historyLog;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
