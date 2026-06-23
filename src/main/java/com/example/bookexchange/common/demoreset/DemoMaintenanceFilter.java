package com.example.bookexchange.common.demoreset;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.web.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class DemoMaintenanceFilter extends OncePerRequestFilter {

    private static final String DEMO_RUNTIME_ENV = "demo";

    private final AppProperties appProperties;
    private final DemoMaintenanceService demoMaintenanceService;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isDemoRuntime()
                || !demoMaintenanceService.isMaintenanceMode()
                || isAlwaysAllowed(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        errorResponseWriter.writeError(request, response, HttpStatus.SERVICE_UNAVAILABLE, MessageKey.SYSTEM_MAINTENANCE);
    }

    private boolean isDemoRuntime() {
        return DEMO_RUNTIME_ENV.equalsIgnoreCase(appProperties.getRuntimeEnv());
    }

    private boolean isAlwaysAllowed(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        return path.equals("/error")
                || path.equals(withApiBasePath("/error"))
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.equals(withApiBasePath("/actuator/health"))
                || path.startsWith(withApiBasePath("/actuator/health/"));
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
}
