package com.example.bookexchange.security.filter;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.admin.api.AdminPaths;
import com.example.bookexchange.security.context.RequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@lombok.RequiredArgsConstructor
public class DeletedModeRequestFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        boolean includeDeleted = isAdminRequest(request);

        RequestContextHolder.setIncludeDeleted(includeDeleted);

        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
        }
    }

    private boolean isAdminRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String servletPath = request.getServletPath();
        String contextPath = request.getContextPath();
        String adminPathWithBaseApi = contextPath + appProperties.getBaseApiPath() + AdminPaths.ADMIN_PATH;
        String adminPathWithoutBaseApi = contextPath + AdminPaths.ADMIN_PATH;

        return requestUri.startsWith(adminPathWithBaseApi)
                || requestUri.startsWith(adminPathWithoutBaseApi)
                || servletPath.startsWith(adminPathWithBaseApi)
                || servletPath.startsWith(adminPathWithoutBaseApi);
    }
}
