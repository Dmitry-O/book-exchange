package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.model.RefreshToken;
import com.example.bookexchange.auth.repository.RefreshTokenRepository;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static com.example.bookexchange.common.i18n.MessageKey.AUTH_REFRESH_TOKEN_EXPIRED;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Test
    void shouldPersistRefreshToken_whenCreateTokenIsCalled() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String token = refreshTokenService.createToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(token).isNotBlank();
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getExpiryDate()).isAfter(Instant.now().plusSeconds(60L * 60L * 24L * 29L));
    }

    @Test
    void shouldReturnFailure_whenValidateTokenFindsExpiredToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("expired-token");
        refreshToken.setExpiryDate(Instant.now().minusSeconds(60));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(refreshToken));

        Result<RefreshToken> result = refreshTokenService.validateToken("expired-token");

        assertFailure(result, AUTH_REFRESH_TOKEN_EXPIRED, BAD_REQUEST);
        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    void shouldDeleteTokenAndLogLogout_whenDeleteTokenBelongsToUser() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-token");
        refreshToken.setUser(user);

        when(refreshTokenRepository.findByTokenAndUserId("valid-token", user.getId())).thenReturn(Optional.of(refreshToken));

        Result<Void> result = refreshTokenService.deleteToken(user.getId(), "valid-token");

        assertThat(result.isSuccess()).isTrue();
        verify(refreshTokenRepository).delete(refreshToken);
        verify(auditService).log(any());
    }

    @Test
    void shouldDeleteUserTokensAndReturnUser_whenDeleteUserTokensIsCalled() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");

        Result<User> result = refreshTokenService.deleteUserTokens(user);

        assertThat(result.isSuccess()).isTrue();
        verify(refreshTokenRepository).deleteByUserId(user.getId());
        verify(auditService, never()).log(any());
    }
}
