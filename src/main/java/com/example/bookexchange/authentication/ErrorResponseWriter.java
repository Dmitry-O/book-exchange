package com.example.bookexchange.authentication;

import com.example.bookexchange.models.MessageKey;
import com.example.bookexchange.services.MessageService;
import com.example.bookexchange.util.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final MessageService messageService;
    private final Helper helper;

    public void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            MessageKey messageKey
    ) throws IOException {
        ResponseEntity<?> apiResponse = helper.formatErrorResponse(
                status,
                messageService.getMessage(messageKey),
                request,
                messageKey.toString()
        );

        response.setContentType("application/json");
        response.setHeader("X-Request-Id", (String) request.getAttribute("requestId"));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.writeValue(response.getOutputStream(), apiResponse.getBody());
    }
}
