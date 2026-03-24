package com.example.bookexchange.authentication;

import com.example.bookexchange.core.audit.AuditEvent;
import com.example.bookexchange.core.audit.AuditResult;
import com.example.bookexchange.core.audit.AuditService;
import com.example.bookexchange.models.MessageKey;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter errorResponseWriter;
    private final AuditService auditService;

    @Override
    public void handle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull AccessDeniedException accessDeniedException) throws IOException, ServletException {
        auditService.log(AuditEvent.builder()
                .action("JWT_FILTERING")
                .result(AuditResult.FAILURE)
                .reason("SYSTEM_ACCESS_FORBIDDEN")
                .build()
        );

        errorResponseWriter.writeError(
                request,
                response,
                HttpStatus.FORBIDDEN,
                MessageKey.SYSTEM_ACCESS_FORBIDDEN
        );
    }
}
