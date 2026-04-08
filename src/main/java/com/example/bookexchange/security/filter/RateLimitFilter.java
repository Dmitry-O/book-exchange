package com.example.bookexchange.security.filter;

import com.example.bookexchange.auth.api.AuthPaths;
import com.example.bookexchange.book.api.BookPaths;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.web.ErrorResponseWriter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ErrorResponseWriter errorResponseWriter;
    private final AuditService auditService;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(50_000)
            .build();

    private enum RateLimitType {
        LOGIN,
        SEARCH,
        EMAIL_FLOW,
        OTHER_AUTH
    }

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(5,
                Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createGetBooksBucket() {
        Bandwidth limit = Bandwidth.classic(100,
                Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createSendEmailAPIsBucket() {
        Bandwidth limit = Bandwidth.classic(2,
                Refill.intervally(2, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createOtherAPIsBucket() {
        Bandwidth limit = Bandwidth.classic(3,
                Refill.intervally(3, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket resolveBucket(String ip, RateLimitType type) {
        String key = ip + ":" + type.name();

        return buckets.get(key, ignored -> createBucket(type));
    }

    private Bucket createBucket(RateLimitType type) {
        return switch (type) {
            case LOGIN -> createLoginBucket();
            case SEARCH -> createGetBooksBucket();
            case EMAIL_FLOW -> createSendEmailAPIsBucket();
            case OTHER_AUTH -> createOtherAPIsBucket();
        };
    }

    private RateLimitType resolveRateLimitType(String path) {
        if (path.endsWith(AuthPaths.AUTH_PATH_LOGIN)) {
            return RateLimitType.LOGIN;
        }

        if (path.endsWith(BookPaths.BOOK_PATH_SEARCH)) {
            return RateLimitType.SEARCH;
        }

        if (path.endsWith(AuthPaths.AUTH_PATH_REGISTER)
                || path.endsWith(AuthPaths.AUTH_PATH_FORGOT_PASSWORD)
                || path.endsWith(AuthPaths.AUTH_PATH_RESEND_CONFIRMATION_EMAIL)) {
            return RateLimitType.EMAIL_FLOW;
        }

        if (path.contains(AuthPaths.AUTH_PATH + "/")) {
            return RateLimitType.OTHER_AUTH;
        }

        return null;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        RateLimitType rateLimitType = resolveRateLimitType(path);

        if (rateLimitType != null) {
            String ip = request.getRemoteAddr();
            Bucket bucket = resolveBucket(ip, rateLimitType);

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

            if (!probe.isConsumed()) {
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;

                response.setHeader("X-Rate-Limit-Retry-After", String.valueOf(waitForRefill));

                errorResponseWriter.writeError(
                        request,
                        response,
                        HttpStatus.TOO_MANY_REQUESTS,
                        MessageKey.SYSTEM_TOO_MANY_REQUESTS
                );

                auditService.log(AuditEvent.builder()
                        .action("RATE_LIMIT_FILTERING")
                        .result(AuditResult.FAILURE)
                        .reason("SYSTEM_TOO_MANY_REQUESTS")
                        .detail("rateLimitType", rateLimitType)
                        .detail("path", path)
                        .detail("ip", ip)
                        .detail("waitForRefill", waitForRefill)
                        .build()
                );

                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
