package com.roomcare.backend.repository;

import com.roomcare.backend.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {
    
    Page<Property> findByLandlordId(Long landlordId, Pageable pageable);
    
    boolean existsByIdAndLandlordId(Long id, Long landlordId);
}
