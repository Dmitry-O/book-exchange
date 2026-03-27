package com.example.bookexchange.security.filter;

import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.web.ErrorResponseWriter;
import com.example.bookexchange.security.auth.CustomUserDetailsServiceImpl;
import com.example.bookexchange.security.auth.JwtService;
import com.example.bookexchange.security.auth.UserPrincipal;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsServiceImpl customUserDetailsService;
    private final AuditService auditService;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);

            return;
        }

        Long userId = null;

        try {
            String token = header.substring(7);
            userId = jwtService.extractUserId(token);

            Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

            if (existingAuth == null || existingAuth instanceof AnonymousAuthenticationToken) {
                UserPrincipal userDetails = customUserDetailsService.loadUserByUserId(userId);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | AuthenticationException ex) {
            SecurityContextHolder.clearContext();

            auditService.log(AuditEvent.builder()
                    .action("JWT_FILTERING")
                    .result(AuditResult.FAILURE)
                    .reason("SYSTEM_INVALID_TOKEN")
                    .detail("exceptionClass", ex.getClass().getSimpleName())
                    .detail("ipAddress", request.getRemoteAddr())
                    .detail("method", request.getMethod())
                    .detail("path", request.getRequestURI())
                    .detail("queryString", request.getQueryString())
                    .detail("userId", userId)
                    .detail("hasBearerHeader", true)
                    .build()
            );

            errorResponseWriter.writeError(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    MessageKey.SYSTEM_INVALID_TOKEN
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}
