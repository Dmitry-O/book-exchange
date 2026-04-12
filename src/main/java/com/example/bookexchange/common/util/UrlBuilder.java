package com.example.bookexchange.common.util;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.email.EmailType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class UrlBuilder {

    private final AppProperties appProperties;

    public String buildEmailActionUrl(String token, EmailType emailType) {
        String path = switch (emailType) {
            case EmailType.CONFIRM_EMAIL -> "/verify-email";
            case EmailType.RESET_PASSWORD -> "/reset-password";
            case EmailType.DELETE_ACCOUNT -> "/delete-account-confirm";
        };

        return trimTrailingSlash(appProperties.getFrontendUrl())
                + path
                + "?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:5173";
        }

        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
