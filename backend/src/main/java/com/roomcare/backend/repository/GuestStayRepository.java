package com.roomcare.backend.repository;

import com.roomcare.backend.entity.GuestStay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuestStayRepository extends JpaRepository<GuestStay, Long> {

    Page<GuestStay> findByTenantId(Long tenantId, Pageable pageable);

    Page<GuestStay> findByTenantIdAndStatus(Long tenantId, GuestStay.Status status, Pageable pageable);

    Optional<GuestStay> findByIdAndTenantId(Long id, Long tenantId);

    Page<GuestStay> findByLandlordId(Long landlordId, Pageable pageable);

    Page<GuestStay> findByLandlordIdAndStatus(Long landlordId, GuestStay.Status status, Pageable pageable);

    Optional<GuestStay> findByIdAndLandlordId(Long id, Long landlordId);
}
