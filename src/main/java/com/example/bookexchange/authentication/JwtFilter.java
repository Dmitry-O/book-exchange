package com.example.bookexchange.authentication;

import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.services.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter{

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);

                return;
            }

            String token = header.substring(7);
            String userId = jwtService.extractUserId(token);

            User user = userRepository.findById(Long.valueOf(userId)).orElse(null);

            if (user != null) {
                List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList();

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        authorities
                );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }

            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write(
                """
                    {
                      "error": "Invalid or expired token"
                    }
                """
            );
        }
    }
}
