package com.roomcare.backend.repository;

import com.roomcare.backend.entity.Contract;
import com.roomcare.backend.entity.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("SELECT c FROM Contract c " +
           "JOIN FETCH c.property " +
           "JOIN FETCH c.room " +
           "JOIN FETCH c.tenant " +
           "WHERE c.landlord.id = :landlordId " +
           "AND (:propertyId IS NULL OR c.property.id = :propertyId) " +
           "AND (:roomId IS NULL OR c.room.id = :roomId) " +
           "AND (:tenantId IS NULL OR c.tenant.id = :tenantId) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "ORDER BY c.createdAt DESC")
    List<Contract> findByLandlordWithFilters(
        @Param("landlordId") Long landlordId,
        @Param("propertyId") Long propertyId,
        @Param("roomId") Long roomId,
        @Param("tenantId") Long tenantId,
        @Param("status") ContractStatus status
    );

    List<Contract> findByRoomIdOrderByStartDateDescCreatedAtDesc(Long roomId);

    List<Contract> findByTenantIdOrderByStartDateDescCreatedAtDesc(Long tenantId);

    Optional<Contract> findFirstByRoomIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(
        Long roomId,
        Collection<ContractStatus> statuses
    );

    Optional<Contract> findFirstByTenantIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(
        Long tenantId,
        Collection<ContractStatus> statuses
    );

    @Query("SELECT c FROM Contract c WHERE c.room.id = :roomId " +
           "AND c.status IN :statuses " +
           "AND (:excludeContractId IS NULL OR c.id <> :excludeContractId) " +
           "AND c.startDate <= :endDate AND c.endDate >= :startDate")
    List<Contract> findOverlappingContracts(
        @Param("roomId") Long roomId,
        @Param("statuses") Collection<ContractStatus> statuses,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("excludeContractId") Long excludeContractId
    );
}
