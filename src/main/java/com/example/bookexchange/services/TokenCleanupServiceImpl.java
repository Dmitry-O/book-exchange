package com.example.bookexchange.services;

import com.example.bookexchange.repositories.RefreshTokenRepository;
import com.example.bookexchange.repositories.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupServiceImpl implements TokenCleanupService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Scheduled(cron = "0 58 15 * * *")
    @Override
    public void deleteExpiredTokens() {
        log.info("Cleaning expired verification tokens");

        int numberOfDeletedVerificationTokens = verificationTokenRepository.deleteExpiredTokens(Instant.now());
        int numberOfDeletedRefreshTokens = refreshTokenRepository.deleteExpiredTokens(Instant.now());

        log.info("Deleted {} expired verification tokens", numberOfDeletedVerificationTokens);
        log.info("Deleted {} expired refresh tokens", numberOfDeletedRefreshTokens);
    }
}
