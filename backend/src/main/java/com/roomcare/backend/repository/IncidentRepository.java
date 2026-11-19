package com.roomcare.backend.repository;

import com.roomcare.backend.entity.Incident;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Page<Incident> findByTenantId(Long tenantId, Pageable pageable);

    Page<Incident> findByTenantIdAndStatus(Long tenantId, Incident.Status status, Pageable pageable);

    Optional<Incident> findByIdAndTenantId(Long id, Long tenantId);

    List<Incident> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incident i where i.id = :id and i.tenant.id = :tenantId")
    Optional<Incident> findForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
