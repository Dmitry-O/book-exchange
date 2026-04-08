package com.example.bookexchange.common.util;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.auth.api.AuthPaths;
import com.example.bookexchange.common.email.EmailType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlBuilder {

    private final AppProperties appProperties;

    public String buildEmailVerificationUrl(String token, EmailType emailType) {
        String base = appProperties.getBaseUrl() + appProperties.getBaseApiPath();

        String endpoint = switch (emailType) {
            case EmailType.CONFIRM_EMAIL -> AuthPaths.AUTH_PATH_CONFIRM_REGISTRATION;
            case EmailType.RESET_PASSWORD -> AuthPaths.AUTH_PATH_RESET_PASSWORD;
            case EmailType.DELETE_ACCOUNT -> AuthPaths.AUTH_PATH_DELETE_ACCOUNT;
        };

        return base + endpoint + "?token=" + token;
    }
}
