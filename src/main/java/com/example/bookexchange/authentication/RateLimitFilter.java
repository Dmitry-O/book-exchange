package com.example.bookexchange.authentication;

import com.example.bookexchange.controllers.AuthController;
import com.example.bookexchange.controllers.BookController;
import com.example.bookexchange.dto.ApiErrorDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

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

        if (path.contains(AuthController.AUTH_PATH) || path.contains(BookController.BOOK_PATH_SEARCH)) {
            String ip = request.getRemoteAddr();
            Bucket bucket = resolveBucket(ip, path);

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

            if (!probe.isConsumed()) {
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;

                response.setHeader("X-Rate-Limit-Retry-After", String.valueOf(waitForRefill));
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");

                String requestId = (String) request.getAttribute("requestId");

                ApiErrorDTO error = ApiErrorDTO.builder()
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .error(HttpStatus.TOO_MANY_REQUESTS.name())
                        .message("Too many requests")
                        .path(path)
                        .timestamp(Instant.now())
                        .requestId(requestId)
                        .build();

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                mapper.writeValue(response.getOutputStream(), error);

                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}