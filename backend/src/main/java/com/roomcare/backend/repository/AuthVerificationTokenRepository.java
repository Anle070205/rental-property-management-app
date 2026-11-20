package com.roomcare.backend.repository;

import com.roomcare.backend.entity.AuthVerificationToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthVerificationTokenRepository extends JpaRepository<AuthVerificationToken, Long> {

    Optional<AuthVerificationToken> findTopByTargetIgnoreCaseAndChannelAndPurposeOrderByCreatedAtDescIdDesc(
            String target,
            AuthVerificationToken.Channel channel,
            AuthVerificationToken.Purpose purpose
    );

    Optional<AuthVerificationToken> findTopByTargetIgnoreCaseAndChannelOrderByCreatedAtDescIdDesc(
            String target,
            AuthVerificationToken.Channel channel
    );

    Optional<AuthVerificationToken> findTopByTargetIgnoreCaseAndChannelAndPurposeAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
            String target,
            AuthVerificationToken.Channel channel,
            AuthVerificationToken.Purpose purpose
    );

    Optional<AuthVerificationToken> findTopByTargetIgnoreCaseAndChannelAndPurposeAndResetTicketHashAndInvalidatedFalseAndUsedAtIsNullOrderByCreatedAtDescIdDesc(
            String target,
            AuthVerificationToken.Channel channel,
            AuthVerificationToken.Purpose purpose,
            String resetTicketHash
    );

    long countByTargetIgnoreCaseAndChannelAndCreatedAtAfter(
            String target,
            AuthVerificationToken.Channel channel,
            LocalDateTime after
    );

    long countByClientIpAndChannelAndCreatedAtAfter(
            String clientIp,
            AuthVerificationToken.Channel channel,
            LocalDateTime after
    );

    List<AuthVerificationToken> findByUserIdAndChannelAndPurposeAndInvalidatedFalseAndUsedAtIsNull(
            Long userId,
            AuthVerificationToken.Channel channel,
            AuthVerificationToken.Purpose purpose
    );
}
