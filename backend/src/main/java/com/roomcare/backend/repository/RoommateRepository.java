package com.roomcare.backend.repository;

import com.roomcare.backend.entity.Roommate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoommateRepository extends JpaRepository<Roommate, Long> {

    List<Roommate> findByLandlordIdOrderByCreatedAtDesc(Long landlordId);

    List<Roommate> findByContractIdOrderByCreatedAtAsc(Long contractId);

    List<Roommate> findByRoomIdOrderByCreatedAtAsc(Long roomId);

    List<Roommate> findByTenantIdOrderByCreatedAtAsc(Long tenantId);

    long countByContractId(Long contractId);

    long countByRoomId(Long roomId);

    long countByTenantId(Long tenantId);
}
