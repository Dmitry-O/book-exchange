package com.example.bookexchange.util;

import com.example.bookexchange.core.error.ApiErrorDTO;
import com.example.bookexchange.core.result.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Helper {

    public ResponseEntity<?> formatErrorResponse(HttpStatus status, String message, HttpServletRequest request, String errorType) {
        String requestId = request.getAttribute("requestId") != null ?
                (String) request.getAttribute("requestId") :
                UUID.randomUUID().toString();

        ApiErrorDTO error = ApiErrorDTO
                .builder()
                .status(status.value())
                .error(errorType)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .error(error)
                .data(null)
                .message(null)
                .build();

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
