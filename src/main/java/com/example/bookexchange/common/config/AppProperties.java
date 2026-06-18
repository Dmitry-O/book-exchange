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
    private String frontendUrl = "http://localhost:5173";
    private String jwtSecretKey;
    private String emailSentFrom;
    private String baseApiPath = "/api/v1";
    private int accessTokenTimeToLive = 54000;
    private List<String> corsAllowedOrigins = List.of("http://localhost:5173");
    private ReportSettings report = new ReportSettings();
    private NotificationSettings notification = new NotificationSettings();
    private ShowcaseSettings showcase = new ShowcaseSettings();
    private DemoEmailSandboxSettings demoEmailSandbox = new DemoEmailSandboxSettings();

    @Getter
    @Setter
    public static class ReportSettings {
        private int snapshotRetentionDays = 180;
    }

    @Getter
    @Setter
    public static class NotificationSettings {
        private long emailBatchDelayMillis = 0;
    }

    @Getter
    @Setter
    public static class ShowcaseSettings {
        private String backendGithubUrl;
        private String frontendGithubUrl;
        private String linkedinUrl;
        private String swaggerUrl;
    }

    @Getter
    @Setter
    public static class DemoEmailSandboxSettings {
        private boolean enabled = false;
        private String mailpitApiBaseUrl = "http://localhost:8025";
        private String mailpitUsername;
        private String mailpitPassword;
        private int sessionTtlMinutes = 180;
        private int maxMessages = 500;
        private boolean deleteExpiredMessages = true;
        private long cleanupIntervalMillis = 300000;
    }
}
