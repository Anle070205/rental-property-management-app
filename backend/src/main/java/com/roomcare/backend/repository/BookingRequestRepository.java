package com.roomcare.backend.repository;

import com.roomcare.backend.entity.BookingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {

    List<BookingRequest> findByTenantUserIdOrderByCreatedAtDesc(Long tenantUserId);

    List<BookingRequest> findByLandlordIdOrderByCreatedAtDesc(Long landlordId);

    List<BookingRequest> findByRoomIdAndRequestTypeAndStatus(
            Long roomId,
            BookingRequest.RequestType type,
            BookingRequest.Status status
    );

    boolean existsByTenantUserIdAndRoomIdAndRequestTypeAndStatus(
            Long tenantUserId,
            Long roomId,
            BookingRequest.RequestType type,
            BookingRequest.Status status
    );

    Optional<BookingRequest> findFirstByRoomIdAndRequestTypeAndStatusOrderByUpdatedAtDesc(
            Long roomId,
            BookingRequest.RequestType type,
            BookingRequest.Status status
    );

    Optional<BookingRequest> findFirstByTenantUserIdAndRequestTypeAndStatusOrderByUpdatedAtDesc(
            Long tenantUserId,
            BookingRequest.RequestType type,
            BookingRequest.Status status
    );

    long countByLandlordIdAndStatus(Long landlordId, BookingRequest.Status status);
}
