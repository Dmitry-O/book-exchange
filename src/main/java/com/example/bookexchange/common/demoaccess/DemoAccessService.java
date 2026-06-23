package com.example.bookexchange.common.demoaccess;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class DemoAccessService {

    private static final String DEMO_RUNTIME_ENV = "demo";
    private static final String COOKIE_VALUE_PREFIX = "demo-cookie:";

    private final AppProperties appProperties;

    public boolean isDemoGateEnabled() {
        return DEMO_RUNTIME_ENV.equalsIgnoreCase(appProperties.getRuntimeEnv());
    }

    public Result<DemoAccessVerificationDTO> verifyAccessToken(
            String token,
            HttpServletResponse response
    ) {
        if (!isDemoGateEnabled()) {
            return ResultFactory.ok(new DemoAccessVerificationDTO(false, null));
        }

        if (!matchesConfiguredTokenHash(token)) {
            return ResultFactory.error(MessageKey.SYSTEM_INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
        }

        Instant expiresAt = Instant.now().plusSeconds(cookieTtlSeconds());
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie().toString());

        return ResultFactory.ok(new DemoAccessVerificationDTO(true, expiresAt));
    }

    public boolean hasValidAccessCookie(HttpServletRequest request) {
        if (!isDemoGateEnabled()) {
            return true;
        }

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return false;
        }

        String cookieName = cookieName();
        String expectedCookieValue = accessCookieValue();

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())
                    && constantTimeEquals(expectedCookieValue, cookie.getValue())) {
                return true;
            }
        }

        return false;
    }

    private ResponseCookie accessCookie() {
        return ResponseCookie.from(cookieName(), accessCookieValue())
                .httpOnly(true)
                .secure(appProperties.getDemoAccess().isSecureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(cookieTtlSeconds()))
                .build();
    }

    private boolean matchesConfiguredTokenHash(String token) {
        String configuredHash = normalizedTokenHash();

        if (configuredHash == null || token == null || token.isBlank()) {
            return false;
        }

        return constantTimeEquals(configuredHash, sha256Hex(token));
    }

    private String accessCookieValue() {
        String configuredHash = normalizedTokenHash();

        if (configuredHash == null) {
            return "";
        }

        return sha256Hex(COOKIE_VALUE_PREFIX + configuredHash);
    }

    private String normalizedTokenHash() {
        String tokenHash = appProperties.getDemoAccess().getTokenHash();

        if (tokenHash == null || tokenHash.isBlank()) {
            return null;
        }

        return tokenHash.trim().toLowerCase();
    }

    private String cookieName() {
        String cookieName = appProperties.getDemoAccess().getCookieName();
        return cookieName == null || cookieName.isBlank() ? "BE_DEMO_ACCESS" : cookieName;
    }

    private long cookieTtlSeconds() {
        return Math.max(60, appProperties.getDemoAccess().getCookieTtlSeconds());
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
