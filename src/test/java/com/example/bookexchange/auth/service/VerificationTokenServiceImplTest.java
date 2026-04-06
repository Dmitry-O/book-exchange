package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.model.TokenType;
import com.example.bookexchange.auth.model.VerificationToken;
import com.example.bookexchange.auth.repository.VerificationTokenRepository;
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

import static com.example.bookexchange.common.i18n.MessageKey.AUTH_TOKEN_EXPIRED;
import static com.example.bookexchange.common.i18n.MessageKey.AUTH_TOKEN_NOT_VALID;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceImplTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private VerificationTokenServiceImpl verificationTokenService;

    @Test
    void shouldPersistConfirmEmailToken_whenCreateTokenIsCalled() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");

        when(verificationTokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Result<String> result = verificationTokenService.createToken(user, TokenType.CONFIRM_EMAIL);

        assertThat(result.isSuccess()).isTrue();

        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getType()).isEqualTo(TokenType.CONFIRM_EMAIL);
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getExpiryDate()).isAfter(Instant.now().plusSeconds(60L * 60L * 23L));
    }

    @Test
    void shouldReturnFailure_whenValidateTokenFindsExpiredVerificationToken() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        VerificationToken token = new VerificationToken();
        token.setId(UnitFixtureIds.VERIFICATION_TOKEN_ID);
        token.setToken("expired-token");
        token.setType(TokenType.CONFIRM_EMAIL);
        token.setUser(user);
        token.setExpiryDate(Instant.now().minusSeconds(60));

        when(verificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        Result<VerificationToken> result = verificationTokenService.validateToken(
                "expired-token",
                TokenType.CONFIRM_EMAIL,
                "CONFIRM_REGISTRATION"
        );

        assertFailure(result, AUTH_TOKEN_EXPIRED, BAD_REQUEST);
        verify(verificationTokenRepository).deleteById(token.getId());
        verify(auditService).log(any());
    }

    @Test
    void shouldReturnFailure_whenValidateTokenTypeDoesNotMatch() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        VerificationToken token = new VerificationToken();
        token.setId(UnitFixtureIds.VERIFICATION_TOKEN_ID);
        token.setToken("wrong-type-token");
        token.setType(TokenType.RESET_PASSWORD);
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(60));

        when(verificationTokenRepository.findByToken("wrong-type-token")).thenReturn(Optional.of(token));

        Result<VerificationToken> result = verificationTokenService.validateToken(
                "wrong-type-token",
                TokenType.CONFIRM_EMAIL,
                "CONFIRM_REGISTRATION"
        );

        assertFailure(result, AUTH_TOKEN_NOT_VALID, BAD_REQUEST);
        verify(auditService).log(any());
    }

    @Test
    void shouldDeleteExistingToken_whenDeleteByUserAndTypeFindsMatch() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        VerificationToken token = new VerificationToken();
        token.setId(UnitFixtureIds.VERIFICATION_TOKEN_ID);

        when(verificationTokenRepository.findByUserAndType(user, TokenType.CONFIRM_EMAIL)).thenReturn(Optional.of(token));

        verificationTokenService.deleteByUserAndType(user, TokenType.CONFIRM_EMAIL);

        verify(verificationTokenRepository).deleteById(token.getId());
    }

    @Test
    void shouldDeleteAllTokensAndReturnUser_whenDeleteUserTokensIsCalled() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");

        Result<User> result = verificationTokenService.deleteUserTokens(user);

        assertThat(result.isSuccess()).isTrue();
        verify(verificationTokenRepository).deleteByUserId(user.getId());
    }
}
