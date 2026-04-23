package com.vsatcompass.api.service;

import com.vsatcompass.api.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nightly cleanup job for refresh tokens.
 * Runs at 03:00 AM daily.
 * Deletes:
 * - Expired tokens (expires_at < now)
 * - Old revoked tokens (revoked=true AND created_at < 7 days ago)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting refresh token cleanup job...");
        int deleted = refreshTokenRepository.deleteExpiredAndOldRevokedTokens();
        log.info("Refresh token cleanup complete. {} tokens removed.", deleted);
    }
}
