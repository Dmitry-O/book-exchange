package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.model.RefreshToken;
import com.example.bookexchange.auth.repository.RefreshTokenRepository;
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
import java.util.HashSet;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    @Override
    public String createToken(User user) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plus(30, DAYS));

        return refreshTokenRepository.save(refreshToken).getToken();
    }

    @Override
    public Result<RefreshToken> validateToken(String token) {
        return ResultFactory.fromOptional(
                        refreshTokenRepository.findByToken(token),
                        MessageKey.AUTH_WRONG_TOKEN
                )
                .flatMap(refreshToken -> {
                    if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                        refreshTokenRepository.delete(refreshToken);

                        return ResultFactory.error(MessageKey.AUTH_REFRESH_TOKEN_EXPIRED, HttpStatus.BAD_REQUEST);
                    }

                    return ResultFactory.ok(refreshToken);
                });
    }

    @Override
    public Result<Void> deleteToken(Long userId, String token) {
        return ResultFactory.fromOptional(
                        refreshTokenRepository.findByTokenAndUserId(token, userId),
                        MessageKey.AUTH_TOKEN_NOT_FOUND
                )
                .flatMap(rt -> {
                    String userEmail = rt.getUser().getEmail();

                    refreshTokenRepository.delete(rt);

                    auditService.log(AuditEvent.builder()
                            .action("LOGOUT_USER")
                            .result(AuditResult.SUCCESS)
                            .actorId(userId)
                            .actorEmail(userEmail)
                            .build()
                    );

                    return ResultFactory.successVoid();
                });
    }

    @Override
    public Result<User> deleteUserTokens(User user) {
        refreshTokenRepository.deleteAll(new HashSet<>(user.getRefreshTokens()));

        return ResultFactory.ok(user);
    }
}
