package com.example.bookexchange.authentication;

import com.example.bookexchange.dto.ApiErrorDTO;
import com.example.bookexchange.models.MessageKey;
import com.example.bookexchange.services.MessageService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final MessageService messageService;

    public JwtAccessDeniedHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void handle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull AccessDeniedException accessDeniedException) throws IOException, ServletException {
        String requestId = (String) request.getAttribute("requestId");

        ApiErrorDTO error = ApiErrorDTO.builder()
                .status(HttpServletResponse.SC_FORBIDDEN)
                .error(HttpStatus.FORBIDDEN.name())
                .message(messageService.getMessage(MessageKey.SYSTEM_ACCESS_FORBIDDEN))
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.writeValue(response.getOutputStream(), error);
    }
}
