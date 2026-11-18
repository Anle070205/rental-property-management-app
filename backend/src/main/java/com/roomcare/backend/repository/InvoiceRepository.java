package com.roomcare.backend.repository;

import com.roomcare.backend.entity.Invoice;
import com.roomcare.backend.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i FROM Invoice i " +
           "JOIN FETCH i.property " +
           "JOIN FETCH i.room " +
           "LEFT JOIN FETCH i.tenant " +
           "WHERE i.landlord.id = :landlordId " +
           "AND (:billingMonth IS NULL OR i.billingMonth = :billingMonth) " +
           "AND (:roomId IS NULL OR i.room.id = :roomId) " +
           "AND (:propertyId IS NULL OR i.property.id = :propertyId) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "ORDER BY i.billingMonth DESC, i.createdAt DESC")
    List<Invoice> findByLandlordWithFilters(
        @Param("landlordId") Long landlordId,
        @Param("billingMonth") String billingMonth,
        @Param("roomId") Long roomId,
        @Param("propertyId") Long propertyId,
        @Param("status") InvoiceStatus status
    );

    @Query("SELECT i FROM Invoice i " +
           "JOIN FETCH i.property " +
           "JOIN FETCH i.room " +
           "LEFT JOIN FETCH i.tenant " +
           "WHERE i.landlord.id = :landlordId " +
           "ORDER BY i.billingMonth DESC, i.createdAt DESC")
    List<Invoice> findAllByLandlordWithDetails(@Param("landlordId") Long landlordId);

    boolean existsByRoomIdAndBillingMonth(Long roomId, String billingMonth);

    Optional<Invoice> findByRoomIdAndBillingMonth(Long roomId, String billingMonth);

    List<Invoice> findByRoomIdAndBillingMonthLessThanAndRemainingAmountGreaterThan(
        Long roomId,
        String billingMonth,
        BigDecimal minRemaining
    );

    List<Invoice> findByRoomIdOrderByBillingMonthDesc(Long roomId);

    List<Invoice> findByLandlordIdAndRemainingAmountGreaterThanOrderByDueDateAsc(
        Long landlordId,
        BigDecimal minRemaining
    );

    @Query("SELECT i FROM Invoice i " +
           "JOIN FETCH i.property " +
           "JOIN FETCH i.room " +
           "LEFT JOIN FETCH i.tenant " +
           "WHERE i.tenant.id = :tenantId AND i.landlord.id = :landlordId " +
           "ORDER BY i.billingMonth DESC, i.dueDate ASC")
    List<Invoice> findByTenantIdAndLandlordIdWithDetails(
        @Param("tenantId") Long tenantId,
        @Param("landlordId") Long landlordId
    );
}
