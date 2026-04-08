package com.example.bookexchange.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private String baseUrl = "http://localhost:8080";
    private String jwtSecretKey;
    private String emailSentFrom;
    private String baseApiPath = "/api/v1";
    private int accessTokenTimeToLive = 54000;
    private List<String> corsAllowedOrigins = List.of("http://localhost:5173");
}
