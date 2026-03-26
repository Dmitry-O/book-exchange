package com.example.bookexchange.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private String baseUrl = "http://localhost:8080";
    private String jwtSecretKey;
    private String emailSentFrom;
    private String baseApiPath = "/api/v1";
}
