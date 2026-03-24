package com.example.bookexchange.authentication;

import com.example.bookexchange.core.audit.AuditEvent;
import com.example.bookexchange.core.audit.AuditResult;
import com.example.bookexchange.core.audit.AuditService;
import com.example.bookexchange.models.MessageKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;
    private final AuditService auditService;

    @Override public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException ex
    ) throws IOException {
        if (ex instanceof InsufficientAuthenticationException) {
            auditService.log(AuditEvent.builder()
                    .action("JWT_FILTERING")
                    .result(AuditResult.FAILURE)
                    .reason("SYSTEM_INVALID_TOKEN")
                    .detail("exception", ex)
                    .build()
            );

            errorResponseWriter.writeError(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    MessageKey.SYSTEM_INVALID_TOKEN
            );

            return;
        }

        errorResponseWriter.writeError(
                request,
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                MessageKey.SYSTEM_UNEXPECTED_ERROR
        );
    }
}