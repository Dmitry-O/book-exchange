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
    private String runtimeEnv = "local";
    private int accessTokenTimeToLive = 54000;
    private List<String> corsAllowedOrigins = List.of("http://localhost:5173");
    private ReportSettings report = new ReportSettings();
    private NotificationSettings notification = new NotificationSettings();
    private ShowcaseSettings showcase = new ShowcaseSettings();
    private DemoAccessSettings demoAccess = new DemoAccessSettings();
    private DemoAccountsSettings demoAccounts = new DemoAccountsSettings();
    private DemoEmailSandboxSettings demoEmailSandbox = new DemoEmailSandboxSettings();
    private DemoResetSettings demoReset = new DemoResetSettings();

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
    public static class DemoAccessSettings {
        private String tokenHash;
        private String cookieName = "BE_DEMO_ACCESS";
        private int cookieTtlSeconds = 604800;
        private boolean secureCookie = true;
    }

    @Getter
    @Setter
    public static class DemoAccountsSettings {
        private String emailPattern = "%.demo@example.com";
        private String password;
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

    @Getter
    @Setter
    public static class DemoResetSettings {
        private boolean enabled = false;
        private String cron = "0 0 0 * * *";
        private String zone = "UTC";
        private String seedS3Key;
        private String s3RuntimePrefix = "runtime/";
        private boolean clearMailpit = true;
        private boolean reindexSearch = true;
    }
}
