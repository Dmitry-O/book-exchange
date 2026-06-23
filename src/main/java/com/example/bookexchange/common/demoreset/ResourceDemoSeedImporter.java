package com.example.bookexchange.common.demoreset;

import com.example.bookexchange.common.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ResourceDemoSeedImporter implements DemoSeedImporter {

    private final DataSource dataSource;
    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @Override
    public void validateSeedAvailable(String seedS3Location) {
        S3SeedLocation s3SeedLocation = parseS3SeedLocation(seedS3Location);

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3SeedLocation.bucket())
                    .key(s3SeedLocation.key())
                    .build());
        } catch (S3Exception ex) {
            throw seedLoadException(s3SeedLocation, ex);
        }
    }

    @Override
    public void importSeed(String seedS3Location) {
        Resource seedResource = loadS3SeedResource(seedS3Location);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding("UTF-8");
        populator.addScript(seedResource);
        populator.execute(dataSource);
    }

    private Resource loadS3SeedResource(String seedS3Location) {
        S3SeedLocation s3SeedLocation = parseS3SeedLocation(seedS3Location);
        ResponseBytes<GetObjectResponse> responseBytes;

        try {
            responseBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(s3SeedLocation.bucket())
                    .key(s3SeedLocation.key())
                    .build());
        } catch (S3Exception ex) {
            throw seedLoadException(s3SeedLocation, ex);
        }

        return new ByteArrayResource(sanitizeSeedSql(responseBytes.asByteArray()), seedS3Location);
    }

    private byte[] sanitizeSeedSql(byte[] seedBytes) {
        String seedSql = new String(seedBytes, StandardCharsets.UTF_8);
        StringBuilder sanitizedSql = new StringBuilder(seedSql.length());

        for (String line : seedSql.split("\\R", -1)) {
            if (isDumpOnlyStatement(line)) {
                continue;
            }

            sanitizedSql.append(line).append(System.lineSeparator());
        }

        return sanitizedSql.toString().getBytes(StandardCharsets.UTF_8);
    }

    private boolean isDumpOnlyStatement(String line) {
        String normalizedLine = line.toUpperCase();

        return normalizedLine.contains("SQL_LOG_BIN")
                || normalizedLine.contains("GTID_PURGED")
                || normalizedLine.contains("MYSQLDUMP_TEMP_LOG_BIN");
    }

    private S3SeedLocation parseS3SeedLocation(String seedS3Location) {
        if (seedS3Location == null || !seedS3Location.startsWith("s3://")) {
            throw new IllegalArgumentException("Invalid S3 demo seed location: " + seedS3Location);
        }

        String withoutScheme = seedS3Location.substring("s3://".length());
        int separatorIndex = withoutScheme.indexOf('/');

        if (separatorIndex <= 0 || separatorIndex == withoutScheme.length() - 1) {
            throw new IllegalArgumentException("Invalid S3 demo seed location: " + seedS3Location);
        }

        String bucket = withoutScheme.substring(0, separatorIndex);
        String key = withoutScheme.substring(separatorIndex + 1);

        if (bucket.isBlank() || key.isBlank()) {
            throw new IllegalArgumentException("Invalid S3 demo seed location: " + seedS3Location);
        }

        return new S3SeedLocation(bucket, key);
    }

    private IllegalStateException seedLoadException(S3SeedLocation s3SeedLocation, S3Exception ex) {
        return new IllegalStateException(
                "Failed to load demo seed from S3. bucket=%s, key=%s, region=%s, status=%s, reason=%s"
                        .formatted(
                                s3SeedLocation.bucket(),
                                s3SeedLocation.key(),
                                storageProperties.getS3().getRegion(),
                                ex.statusCode(),
                                ex.awsErrorDetails() != null
                                        ? ex.awsErrorDetails().errorMessage()
                                        : ex.getMessage()
                        ),
                ex
        );
    }

    private record S3SeedLocation(String bucket, String key) {
    }
}
