package com.example.bookexchange.common.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3StorageConfig {

    @Bean
    @ConditionalOnMissingBean(S3Client.class)
    public S3Client s3Client(StorageProperties storageProperties) {
        return S3Client.builder()
                .region(Region.of(storageProperties.getS3().getRegion()))
                .build();
    }
}
