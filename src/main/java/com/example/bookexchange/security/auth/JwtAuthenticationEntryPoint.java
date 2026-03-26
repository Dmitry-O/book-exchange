package com.example.bookexchange.security.auth;

import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.web.ErrorResponseWriter;
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