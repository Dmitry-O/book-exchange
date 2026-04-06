package com.example.bookexchange.common.jobs;

import com.example.bookexchange.auth.repository.RefreshTokenRepository;
import com.example.bookexchange.auth.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceImplTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private TokenCleanupServiceImpl tokenCleanupService;

    @Test
    void shouldTriggerCleanupForBothRepositories_whenDeleteExpiredTokensRuns() {
        when(verificationTokenRepository.deleteExpiredTokens(any())).thenReturn(3);
        when(refreshTokenRepository.deleteExpiredTokens(any())).thenReturn(4);

        tokenCleanupService.deleteExpiredTokens();

        verify(verificationTokenRepository).deleteExpiredTokens(any());
        verify(refreshTokenRepository).deleteExpiredTokens(any());
    }
}
