package com.roomcare.backend.repository;

import com.roomcare.backend.entity.UtilityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilityRecordRepository extends JpaRepository<UtilityRecord, Long> {

    @Query("SELECT u FROM UtilityRecord u " +
           "JOIN FETCH u.property " +
           "JOIN FETCH u.room " +
           "WHERE u.landlord.id = :landlordId " +
           "AND (:billingMonth IS NULL OR u.billingMonth = :billingMonth) " +
           "AND (:roomId IS NULL OR u.room.id = :roomId) " +
           "AND (:propertyId IS NULL OR u.property.id = :propertyId) " +
           "ORDER BY u.billingMonth DESC, u.room.roomCode ASC")
    List<UtilityRecord> findByLandlordWithFilters(
        @Param("landlordId") Long landlordId,
        @Param("billingMonth") String billingMonth,
        @Param("roomId") Long roomId,
        @Param("propertyId") Long propertyId
    );

    Optional<UtilityRecord> findByRoomIdAndBillingMonth(Long roomId, String billingMonth);

    boolean existsByRoomIdAndBillingMonth(Long roomId, String billingMonth);

    Optional<UtilityRecord> findTopByRoomIdAndBillingMonthLessThanOrderByBillingMonthDesc(
        Long roomId,
        String billingMonth
    );

    List<UtilityRecord> findByRoomIdOrderByBillingMonthDesc(Long roomId);
}
