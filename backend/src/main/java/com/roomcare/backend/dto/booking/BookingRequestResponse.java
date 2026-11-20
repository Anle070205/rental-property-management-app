package com.roomcare.backend.dto.booking;

import com.roomcare.backend.entity.BookingRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestResponse {
    private Long id;
    private Long roomId;
    private String roomCode;
    private Long propertyId;
    private String propertyName;
    private String propertyAddress;
    private BigDecimal rentPrice;
    private BigDecimal deposit;
    private Integer roomHealthScore;
    private Long landlordId;
    private String landlordName;
    private String landlordPhone;
    private Long tenantUserId;
    private String tenantName;
    private String tenantPhone;
    private String tenantEmail;
    private String identityNumber;
    private BookingRequest.RequestType requestType;
    private BookingRequest.Status status;
    private LocalDate moveInDate;
    private String message;
    private String landlordNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
