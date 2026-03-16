package com.example.bookexchange.util;

import com.example.bookexchange.config.AppProperties;
import com.example.bookexchange.controllers.AdminController;
import com.example.bookexchange.controllers.AuthController;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.models.EmailType;
import com.example.bookexchange.models.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlBuilder {

    private final AppProperties appProperties;

    public String buildReportTargetUrl(TargetType type, Long id) {
        String base = appProperties.getBaseUrl();

        return switch (type) {
            case USER -> base + AdminController.ADMIN_PATH_USERS + "/" + id;
            case BOOK -> base + AdminController.ADMIN_PATH_BOOKS + "/" + id;
        };
    }

    public String buildEmailVerificationUrl(String token, EmailType emailType) {
        String base = appProperties.getBaseUrl();
        String endpoint = switch (emailType) {
            case EmailType.CONFIRM_EMAIL -> AuthController.AUTH_PATH_CONFIRM_REGISTRATION;
            case EmailType.RESET_PASSWORD -> AuthController.AUTH_PATH_RESET_PASSWORD;
            case EmailType.DELETE_ACCOUNT -> AuthController.AUTH_PATH_DELETE_ACCOUNT;
            default -> throw new BadRequestException("Es wurde ein ungültiger E-Mail-Typ angegeben");
        };

        return base + endpoint + "?token=" + token;
    }
}
