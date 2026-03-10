package com.example.bookexchange.exception;

import com.example.bookexchange.dto.ApiErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorDTO> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute("requestId");

        ApiErrorDTO error = ApiErrorDTO
                .builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute("requestId");

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiErrorDTO error = ApiErrorDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorDTO> handleApiException(ApiException ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute("requestId");

        ApiErrorDTO error = ApiErrorDTO
                .builder()
                .status(ex.getStatus().value())
                .error(ex.getStatus().name())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        return ResponseEntity
                .status(ex.getStatus())
                .body(error);
    }

    @ExceptionHandler
    ResponseEntity<ApiErrorDTO> handleJPAViolations(TransactionSystemException ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute("requestId");
        String message = "Bei der Verarbeitung Ihrer Daten ist ein unerwarteter Fehler aufgetreten";

        if (ex.getCause().getCause() instanceof ConstraintViolationException constraintViolationException) {
            message = constraintViolationException.getConstraintViolations()
                    .stream()
                    .map(e -> e.getPropertyPath().toString() + ": " + e.getMessage())
                    .collect(Collectors.joining(", "));
        }

        ApiErrorDTO error = ApiErrorDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }
}