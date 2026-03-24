package com.example.bookexchange.authentication;

import com.example.bookexchange.core.audit.AuditEvent;
import com.example.bookexchange.core.audit.AuditResult;
import com.example.bookexchange.core.audit.AuditService;
import com.example.bookexchange.models.UserPrincipal;
import com.example.bookexchange.services.CustomUserDetailsServiceImpl;
import com.example.bookexchange.services.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

        try {
            String token = header.substring(7);
            Long userId = jwtService.extractUserId(token);

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
        } catch (JwtException ex) {
            auditService.log(AuditEvent.builder()
                    .action("JWT_FILTERING")
                    .result(AuditResult.FAILURE)
                    .reason("ACCESS_FAILURE")
                    .detail("authHader", header)
                    .detail("exception", ex)
                    .build()
            );

            throw new BadRequestException();
        }

        filterChain.doFilter(request, response);
    }
}
