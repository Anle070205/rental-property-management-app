package com.roomcare.backend.repository;

import com.roomcare.backend.entity.Tenant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByUserId(Long userId);

    @Query("SELECT t FROM Tenant t WHERE (:email IS NOT NULL AND :email <> '' AND LOWER(t.email) = LOWER(:email)) " +
           "OR (:phone IS NOT NULL AND :phone <> '' AND t.phone = :phone)")
    Optional<Tenant> findFirstByEmailIgnoreCaseOrPhone(@Param("email") String email, @Param("phone") String phone);

    Optional<Tenant> findFirstByPhoneIn(Collection<String> phones);

    boolean existsByPhoneIn(Collection<String> phones);

    Optional<Tenant> findFirstByPropertyIdOrderByIdAsc(Long propertyId);
    
    Page<Tenant> findByLandlordId(Long landlordId, Pageable pageable);
    
    @Query("SELECT t FROM Tenant t WHERE t.landlord.id = :landlordId " +
           "AND (:propertyId IS NULL OR t.property.id = :propertyId) " +
           "AND (:keyword IS NULL OR LOWER(t.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(t.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Tenant> findByLandlordIdWithFilters(
        @Param("landlordId") Long landlordId,
        @Param("propertyId") Long propertyId,
        @Param("keyword") String keyword,
        Pageable pageable
    );
    
    boolean existsByPhone(String phone);
    
    boolean existsByIdentityNumber(String identityNumber);
    
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Tenant t " +
           "WHERE t.id = :tenantId AND t.landlord.id = :landlordId")
    boolean existsByIdAndLandlordId(@Param("tenantId") Long tenantId, @Param("landlordId") Long landlordId);
}
