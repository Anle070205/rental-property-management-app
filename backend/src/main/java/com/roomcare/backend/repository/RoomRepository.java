package com.roomcare.backend.repository;

import com.roomcare.backend.entity.Room;
import com.roomcare.backend.entity.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    Page<Room> findByPropertyId(Long propertyId, Pageable pageable);
    
    @Query("SELECT r FROM Room r WHERE r.property.landlord.id = :landlordId")
    Page<Room> findByLandlordId(@Param("landlordId") Long landlordId, Pageable pageable);
    
    @Query("SELECT r FROM Room r WHERE r.property.id = :propertyId " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:minPrice IS NULL OR r.rentPrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR r.rentPrice <= :maxPrice) " +
           "AND (:floor IS NULL OR r.floor = :floor)")
    Page<Room> findByPropertyIdWithFilters(
        @Param("propertyId") Long propertyId,
        @Param("status") RoomStatus status,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("floor") Integer floor,
        Pageable pageable
    );
    
    boolean existsByPropertyIdAndRoomCode(Long propertyId, String roomCode);
    
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r " +
           "WHERE r.id = :roomId AND r.property.landlord.id = :landlordId")
    boolean existsByIdAndLandlordId(@Param("roomId") Long roomId, @Param("landlordId") Long landlordId);
}
