package com.example.bookexchange.authentication;

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

    @Override public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException ex
    ) throws IOException {
        if (ex instanceof InsufficientAuthenticationException) {
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