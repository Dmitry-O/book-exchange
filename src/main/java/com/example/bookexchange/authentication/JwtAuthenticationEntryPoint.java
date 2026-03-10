package com.example.bookexchange.authentication;

import com.example.bookexchange.dto.ApiErrorDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException ex
    ) throws IOException {
        String message = "Invalid or expired token";
        String requestId = (String) request.getAttribute("requestId");

        if (ex instanceof BadCredentialsException) {
            message = ex.getMessage();
        }

        ApiErrorDTO error = ApiErrorDTO.builder()
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .error(HttpStatus.UNAUTHORIZED.name())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.writeValue(response.getOutputStream(), error);
    }
}