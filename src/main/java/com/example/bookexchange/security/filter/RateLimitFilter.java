package com.example.bookexchange.security.filter;

import com.example.bookexchange.auth.api.AuthPaths;
import com.example.bookexchange.book.api.BookPaths;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.web.ErrorResponseWriter;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ErrorResponseWriter errorResponseWriter;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AuditService auditService;

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(5,
                Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createGetBooksBucket() {
        Bandwidth limit = Bandwidth.classic(7,
                Refill.intervally(7, Duration.ofMinutes(1)));
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

    private Bucket resolveBucket(String ip, String path) {
        String endpointName = path.split("/")[4];

        return switch (endpointName) {
            case "login" -> buckets.computeIfAbsent(ip, k -> createLoginBucket());
            case "search" -> buckets.computeIfAbsent(ip, k -> createGetBooksBucket());
            case "register", "forgot_password", "resend_confirmation_email" ->
                    buckets.computeIfAbsent(ip, k -> createSendEmailAPIsBucket());
            default -> buckets.computeIfAbsent(ip, k -> createOtherAPIsBucket());
        };
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.contains(AuthPaths.AUTH_PATH) || path.contains(BookPaths.BOOK_PATH_SEARCH)) {
            String ip = request.getRemoteAddr();
            Bucket bucket = resolveBucket(ip, path);

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
                        .detail("path", path)
                        .detail("ip", ip)
                        .detail("path", path)
                        .detail("waitForRefill", waitForRefill)
                        .build()
                );

                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}