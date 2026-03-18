package com.example.bookexchange.exception;

import com.example.bookexchange.dto.ApiErrorDTO;
import com.example.bookexchange.models.MessageKey;
import com.example.bookexchange.services.MessageService;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorDTO> handleApiException(
            ApiException ex,
            HttpServletRequest request
    ) {
        String requestId = (String) request.getAttribute("requestId");

        MessageKey messageKey = ex.getMessageKey();
        HttpStatus httpStatus = ex.getStatus();

        String message = messageService.getMessage(
                messageKey,
                ex.getArgs()
        );

        ApiErrorDTO error = ApiErrorDTO.builder()
                .status(httpStatus.value())
                .error(messageKey.toString())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        return ResponseEntity
                .status(httpStatus)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleUnknown(HttpServletRequest request) {
        String requestId = (String) request.getAttribute("requestId");

        ApiErrorDTO error = ApiErrorDTO
                .builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .message(messageService.getMessage(MessageKey.SYSTEM_UNEXPECTED_ERROR))
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> messages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    String fieldName = messageService.getMessage("field." + error.getField());

                    return fieldName + " " + error.getDefaultMessage();
                })
                .toList();

        String finalMessage = !messages.isEmpty() ? String.join("; ", messages) : messageService.getMessage(MessageKey.SYSTEM_INVALID_DATA);

        ApiErrorDTO error = ApiErrorDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message(finalMessage)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler
    ResponseEntity<ApiErrorDTO> handleJPAViolations(TransactionSystemException ex, HttpServletRequest request) {
        String requestId = (String) request.getAttribute("requestId");
        String message = messageService.getMessage(MessageKey.SYSTEM_UNEXPECTED_DB_ERROR);

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

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<?> handleOptimisticLock(
            HttpServletRequest request
    ) {
        String requestId = (String) request.getAttribute("requestId");
        String message = messageService.getMessage(MessageKey.SYSTEM_OPTIMISTIC_LOCK);

        ApiErrorDTO error = ApiErrorDTO.builder()
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.name())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .requestId(requestId)
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }
}