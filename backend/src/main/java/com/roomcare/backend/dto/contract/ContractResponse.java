package com.roomcare.backend.dto.contract;

import com.roomcare.backend.dto.roommate.RoommateResponse;
import com.roomcare.backend.entity.ContractStatus;
import com.roomcare.backend.entity.WaterBillingMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponse {
    private Long id;
    private String contractCode;
    private Long landlordId;
    private String landlordName;
    private String landlordPhone;
    private Long propertyId;
    private String propertyName;
    private String propertyAddress;
    private Long roomId;
    private String roomCode;
    private Long tenantId;
    private String tenantName;
    private String tenantPhone;
    private String tenantEmail;
    private Long tenantUserId;
    private String tenantAccountStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal depositAmount;
    private BigDecimal electricityUnitPrice;
    private BigDecimal waterUnitPrice;
    private WaterBillingMode waterBillingMode;
    private BigDecimal internetFee;
    private BigDecimal garbageFee;
    private BigDecimal serviceFee;
    private Integer paymentDayOfMonth;
    private String terms;
    private String documentUrl;
    private ContractStatus status;
    private LocalDateTime signedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime terminatedAt;
    private String terminationReason;
    private int occupantCount;
    private List<RoommateResponse> roommates = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
