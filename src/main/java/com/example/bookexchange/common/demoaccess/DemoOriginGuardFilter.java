package com.example.bookexchange.common.demoaccess;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.web.ErrorResponseWriter;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class DemoOriginGuardFilter extends OncePerRequestFilter {

    private static final String DEMO_RUNTIME_ENV = "demo";

    private final AppProperties appProperties;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isGuardEnabled()
                || isHealthRequest(request)
                || hasValidOriginHeader(request)
        ) {
            filterChain.doFilter(request, response);

            return;
        }

        errorResponseWriter.writeError(
                request,
                response,
                HttpStatus.FORBIDDEN,
                MessageKey.SYSTEM_ACCESS_FORBIDDEN
        );
    }

    private boolean isGuardEnabled() {
        return DEMO_RUNTIME_ENV.equalsIgnoreCase(appProperties.getRuntimeEnv())
                && appProperties.getDemoOriginGuard().isEnabled();
    }

    private boolean hasValidOriginHeader(HttpServletRequest request) {
        String expectedValue = appProperties.getDemoOriginGuard().getHeaderValue();
        String headerName = headerName();

        if (expectedValue == null || expectedValue.isBlank() || headerName.isBlank()) {
            return false;
        }

        return constantTimeEquals(expectedValue.trim(), request.getHeader(headerName));
    }

    private String headerName() {
        String headerName = appProperties.getDemoOriginGuard().getHeaderName();

        return headerName == null || headerName.isBlank() ? "X-Origin-Verify" : headerName.trim();
    }

    private boolean isHealthRequest(HttpServletRequest request) {
        String path = stripContextPath(request);
        String apiHealthPath = withApiBasePath("/actuator/health");

        return isHealthPath(path)
                || path.equals(apiHealthPath)
                || path.startsWith(apiHealthPath + "/");
    }

    private boolean isHealthPath(String path) {
        return path.equals("/actuator/health") || path.startsWith("/actuator/health/");
    }

    private String stripContextPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }

        return path;
    }

    private String withApiBasePath(String path) {
        String basePath = appProperties.getBaseApiPath();

        if (basePath == null || basePath.isBlank() || "/".equals(basePath)) {
            return path;
        }

        String normalizedBasePath = basePath.startsWith("/") ? basePath : "/" + basePath;

        if (normalizedBasePath.endsWith("/")) {
            normalizedBasePath = normalizedBasePath.substring(0, normalizedBasePath.length() - 1);
        }

        return normalizedBasePath + path;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
