package com.roomcare.backend.repository;

import com.roomcare.backend.entity.AuthVerificationToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthVerificationTokenRepository extends JpaRepository<AuthVerificationToken, Long> {

    Optional<AuthVerificationToken> findTopByEmailIgnoreCaseAndPurposeOrderByCreatedAtDescIdDesc(
            String email,
            AuthVerificationToken.Purpose purpose
    );

    Optional<AuthVerificationToken> findTopByEmailIgnoreCaseAndPurposeAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
            String email,
            AuthVerificationToken.Purpose purpose
    );

    Optional<AuthVerificationToken> findTopByEmailIgnoreCaseAndPurposeAndResetTicketHashAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
            String email,
            AuthVerificationToken.Purpose purpose,
            String resetTicketHash
    );

    long countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(
            String email,
            AuthVerificationToken.Purpose purpose,
            LocalDateTime after
    );

    List<AuthVerificationToken> findByUserIdAndPurposeAndInvalidatedFalseAndUsedAtIsNull(
            Long userId,
            AuthVerificationToken.Purpose purpose
    );
}
