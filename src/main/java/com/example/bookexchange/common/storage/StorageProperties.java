package com.example.bookexchange.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class StorageProperties {

    private S3 s3 = new S3();
    private Image image = new Image();
    private Cleanup cleanup = new Cleanup();

    @Getter
    @Setter
    public static class S3 {
        private String region = "eu-central-1";
        private String testBucket = "book-exchange-test";
        private String prodBucket = "book-exchange-prod";
        private boolean useTestBucket = false;
    }

    @Getter
    @Setter
    public static class Image {
        private int maxWidth = 1200;
        private int maxHeight = 1200;
        private float jpegQuality = 0.82F;
    }

    @Getter
    @Setter
    public static class Cleanup {
        private int softDeletedBookPhotoRetentionDays = 30;
    }
}
