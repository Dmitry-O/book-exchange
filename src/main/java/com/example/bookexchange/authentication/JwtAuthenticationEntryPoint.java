package com.example.bookexchange.authentication;

import com.example.bookexchange.dto.ApiErrorDTO;
import com.example.bookexchange.models.MessageKey;
import com.example.bookexchange.services.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final MessageService messageService;

    @Override public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException ex
    ) throws IOException {
        String message = messageService.getMessage(MessageKey.SYSTEM_UNEXPECTED_ERROR);
        String requestId = (String) request.getAttribute("requestId");

        if (ex instanceof BadCredentialsException) {
            message = ex.getMessage();
        }

        ApiErrorDTO error = ApiErrorDTO.builder()
                .status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                .error(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.writeValue(response.getOutputStream(), error);
    }
}