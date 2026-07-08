package com.example.bookexchange.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void shouldUseConfiguredServerUrl() {
        OpenAPI openAPI = openApiConfig.customOpenAPI("/api/v1");

        assertThat(openAPI.getServers())
                .singleElement()
                .satisfies(server -> assertThat(server.getUrl()).isEqualTo("/api/v1"));
    }
}
