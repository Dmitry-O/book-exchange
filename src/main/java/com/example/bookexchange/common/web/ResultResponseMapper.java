package com.example.bookexchange.common.web;

import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Failure;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.Success;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.i18n.MessageService;
import com.example.bookexchange.common.util.ErrorHelper;
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

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
public class ResultResponseMapper {

    private final MessageService messageService;
    private final ErrorHelper errorHelper;
    private final AuditService auditService;

    public ResponseEntity<?> map(Result<?> result, HttpServletRequest request) {
        if (result instanceof Success<?> success) {
            String message = success.messageKey() != null
                    ? messageService.getMessage(success.messageKey())
                    : null;

            ApiResponse<?> response = ApiResponse.builder()
                    .success(true)
                    .data(success.body())
                    .message(message)
                    .error(null)
                    .build();

            return ResponseEntity
                    .status(success.status())
                    .eTag(success.eTag())
                    .body(response);
        }

        if (result instanceof Failure<?> failure) {
            return errorHelper.formatErrorResponse(
                    failure.status(),
                    messageService.getMessage(
                            failure.messageKey(),
                            failure.args()
                    ),
                    request,
                    failure.messageKey().toString()
            );
        }

        auditService.log(AuditEvent.builder()
                .action("RESPONSE_ENTITY_MAPPING")
                .result(AuditResult.ERROR)
                .reason("WRONG_RESULT_TYPE_PROVIDED")
                .build()
        );

        return errorHelper.formatErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                messageService.getMessage(MessageKey.SYSTEM_UNEXPECTED_ERROR),
                request,
                MessageKey.SYSTEM_UNEXPECTED_ERROR.toString()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnknown(HttpServletRequest request, Exception ex) {
        auditService.log(AuditEvent.builder()
                .action("UNKNOWN_EXCEPTION_HANDLING")
                .result(AuditResult.ERROR)
                .reason("SYSTEM_UNEXPECTED_ERROR")
                .detail("exception", ex)
                .build()
        );

        return errorHelper.formatErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                messageService.getMessage(MessageKey.SYSTEM_UNEXPECTED_ERROR),
                request,
                MessageKey.SYSTEM_UNEXPECTED_ERROR.toString()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(
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

        return errorHelper.formatErrorResponse(
                HttpStatus.BAD_REQUEST,
                finalMessage,
                request,
                "VALIDATION_ERROR"
        );
    }

    @ExceptionHandler
    ResponseEntity<?> handleJPAViolations(TransactionSystemException ex, HttpServletRequest request) {
        auditService.log(AuditEvent.builder()
                .action("JPA_VIOLATIONS_HANDLING")
                .result(AuditResult.ERROR)
                .reason("SYSTEM_UNEXPECTED_DB_ERROR")
                .detail("exception", ex)
                .build()
        );

        String message = messageService.getMessage(MessageKey.SYSTEM_UNEXPECTED_DB_ERROR);

        if (ex.getCause().getCause() instanceof ConstraintViolationException constraintViolationException) {
            message = constraintViolationException.getConstraintViolations()
                    .stream()
                    .map(e -> e.getPropertyPath().toString() + ": " + e.getMessage())
                    .collect(Collectors.joining(", "));
        }

        return errorHelper.formatErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request,
                MessageKey.SYSTEM_UNEXPECTED_DB_ERROR.toString()
        );
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<?> handleOptimisticLock(
            HttpServletRequest request,
            Exception ex
    ) {
        auditService.log(AuditEvent.builder()
                .action("OPTIMISTIC_LOCK_HANDLING")
                .result(AuditResult.FAILURE)
                .reason("SYSTEM_OPTIMISTIC_LOCK")
                .detail("exception", ex)
                .build()
        );

        String message = messageService.getMessage(MessageKey.SYSTEM_OPTIMISTIC_LOCK);

        return errorHelper.formatErrorResponse(
                HttpStatus.CONFLICT,
                message,
                request,
                HttpStatus.CONFLICT.name()
        );
    }
}
