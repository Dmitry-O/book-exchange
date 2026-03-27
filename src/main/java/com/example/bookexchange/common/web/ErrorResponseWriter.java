package com.example.bookexchange.common.web;

import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.i18n.MessageService;
import com.example.bookexchange.common.util.ErrorHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

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
        response.setStatus(status.value());

        objectMapper.writeValue(response.getOutputStream(), apiResponse.getBody());
    }
}
