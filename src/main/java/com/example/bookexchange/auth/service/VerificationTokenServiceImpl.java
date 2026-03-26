package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.model.TokenType;
import com.example.bookexchange.auth.model.VerificationToken;
import com.example.bookexchange.auth.repository.VerificationTokenRepository;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

@Service
@RequiredArgsConstructor
public class VerificationTokenServiceImpl implements VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final AuditService auditService;

    @Override
    public Result<String> createToken(User user, TokenType tokenType) {
        VerificationToken verificationToken = new VerificationToken();

        long tokenLiveTime;
        ChronoUnit timeUnit;

        switch (tokenType) {
            case CONFIRM_EMAIL -> {
                tokenLiveTime = 24L;
                timeUnit = HOURS;
            }
            case RESET_PASSWORD, DELETE_ACCOUNT -> {
                tokenLiveTime = 15L;
                timeUnit = MINUTES;
            }
            default -> {
                return ResultFactory.error(
                        MessageKey.AUTH_WRONG_TOKEN,
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setExpiryDate(Instant.now().plus(tokenLiveTime, timeUnit));
        verificationToken.setType(tokenType);
        verificationToken.setUser(user);

        return ResultFactory.ok(verificationTokenRepository.save(verificationToken).getToken());
    }

    @Override
    public Result<VerificationToken> validateToken(String token, TokenType expectedType, String action) {
        return ResultFactory.fromOptional(
                        verificationTokenRepository.findByToken(token),
                        MessageKey.AUTH_TOKEN_NOT_FOUND
                )
                .flatMap(verificationToken -> {
                    if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
                        verificationTokenRepository.deleteById(verificationToken.getId());

                        logFailure(action, verificationToken.getUser(), "AUTH_TOKEN_EXPIRED");

                        return ResultFactory.error(MessageKey.AUTH_TOKEN_EXPIRED, HttpStatus.BAD_REQUEST);
                    }

                    if (!verificationToken.getType().equals(expectedType)) {
                        logFailure(action, verificationToken.getUser(), "AUTH_TOKEN_NOT_VALID");

                        return ResultFactory.error(MessageKey.AUTH_TOKEN_NOT_VALID, HttpStatus.BAD_REQUEST);
                    }

                    return ResultFactory.ok(verificationToken);
                });
    }

    @Override
    public void deleteToken(VerificationToken verificationToken) {
        verificationTokenRepository.deleteById(verificationToken.getId());
    }

    @Override
    public void deleteByUserAndType(User user, TokenType tokenType) {
        verificationTokenRepository.findByUserAndType(user, tokenType)
                .ifPresent(token -> verificationTokenRepository.deleteById(token.getId()));
    }

    private void logFailure(String action, User user, String reason) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.FAILURE)
                .actorId(user.getId())
                .actorEmail(user.getEmail())
                .reason(reason)
                .build()
        );
    }

    @Override
    public Result<User> deleteUserTokens(User user) {
        verificationTokenRepository.deleteAll(new HashSet<>(user.getVerificationToken()));

        return ResultFactory.ok(user);
    }
}
