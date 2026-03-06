package com.example.bookexchange.util;

import com.example.bookexchange.config.AppProperties;
import com.example.bookexchange.controllers.AdminController;
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
}
