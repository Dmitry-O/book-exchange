package com.example.bookexchange.common.web;

import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.i18n.MessageService;
import com.example.bookexchange.common.util.ErrorHelper;
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
public final class ErrorResponseWriter {

    private final MessageService messageService;
    private final ErrorHelper errorHelper;

    public void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            MessageKey messageKey
    ) throws IOException {
        ResponseEntity<?> apiResponse = errorHelper.formatErrorResponse(
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
