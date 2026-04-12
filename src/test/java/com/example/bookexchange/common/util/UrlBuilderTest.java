package com.example.bookexchange.common.util;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.email.EmailType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlBuilderTest {

    @Test
    void shouldBuildFrontendVerificationUrl() {
        UrlBuilder urlBuilder = new UrlBuilder(appProperties("http://localhost:5173"));

        String url = urlBuilder.buildEmailActionUrl("confirm-token", EmailType.CONFIRM_EMAIL);

        assertThat(url).isEqualTo("http://localhost:5173/verify-email?token=confirm-token");
    }

    @Test
    void shouldBuildFrontendResetPasswordUrl_whenFrontendBaseHasTrailingSlash() {
        UrlBuilder urlBuilder = new UrlBuilder(appProperties("http://localhost:5173/"));

        String url = urlBuilder.buildEmailActionUrl("reset+token", EmailType.RESET_PASSWORD);

        assertThat(url).isEqualTo("http://localhost:5173/reset-password?token=reset%2Btoken");
    }

    @Test
    void shouldBuildFrontendDeleteAccountUrl() {
        UrlBuilder urlBuilder = new UrlBuilder(appProperties("https://book-exchange.example"));

        String url = urlBuilder.buildEmailActionUrl("delete-token", EmailType.DELETE_ACCOUNT);

        assertThat(url).isEqualTo("https://book-exchange.example/delete-account-confirm?token=delete-token");
    }

    private AppProperties appProperties(String frontendUrl) {
        AppProperties appProperties = new AppProperties();
        appProperties.setFrontendUrl(frontendUrl);
        return appProperties;
    }
}
