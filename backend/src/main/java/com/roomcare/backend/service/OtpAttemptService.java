package com.roomcare.backend.service;

import com.roomcare.backend.entity.AuthVerificationToken;
import com.roomcare.backend.repository.AuthVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi nhận số lần nhập sai OTP trong transaction độc lập để không bị rollback
 * khi luồng kiểm tra ném RuntimeException.
 */
@Service
@RequiredArgsConstructor
public class OtpAttemptService {

    private final AuthVerificationTokenRepository tokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailedAttempt(Long tokenId) {
        AuthVerificationToken token = tokenRepository.findById(tokenId).orElse(null);
        if (token == null) {
            return 0;
        }
        int nextAttempts = (token.getAttemptCount() != null ? token.getAttemptCount() : 0) + 1;
        int maxAttempts = token.getMaxAttempts() != null && token.getMaxAttempts() > 0 ? token.getMaxAttempts() : 5;
        token.setAttemptCount(nextAttempts);
        if (nextAttempts >= maxAttempts) {
            token.setInvalidated(true);
        }
        tokenRepository.save(token);
        return Math.max(0, maxAttempts - nextAttempts);
    }
}
