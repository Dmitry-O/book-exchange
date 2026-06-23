package com.example.bookexchange.common.demoreset;

import com.example.bookexchange.common.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceDemoSeedImporterTest {

    @Mock
    private S3Client s3Client;

    private JdbcTemplate jdbcTemplate;
    private ResourceDemoSeedImporter importer;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:demo_seed_importer_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.getS3().setRegion("eu-central-1");
        importer = new ResourceDemoSeedImporter(dataSource, s3Client, storageProperties);
    }

    @Test
    void shouldImportSeedFromS3Location() {
        String sql = """
                CREATE TABLE demo_import (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(50) NOT NULL
                );
                INSERT INTO demo_import (id, name) VALUES (1, 'seed');
                """;
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                sql.getBytes(StandardCharsets.UTF_8)
        ));

        importer.importSeed("s3://demo-bucket/private/demo-reset/demo-seed.sql");

        assertThat(jdbcTemplate.queryForObject("SELECT name FROM demo_import WHERE id = 1", String.class))
                .isEqualTo("seed");

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("demo-bucket");
        assertThat(requestCaptor.getValue().key()).isEqualTo("private/demo-reset/demo-seed.sql");
    }

    @Test
    void shouldIgnoreMysqlDumpOnlyStatements_whenImportingSeed() {
        String sql = """
                CREATE TABLE demo_dump_import (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(50) NOT NULL
                );
                SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
                SET @@SESSION.SQL_LOG_BIN= 0;
                SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '';
                INSERT INTO demo_dump_import (id, name) VALUES (1, 'seed');
                SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
                """;
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                sql.getBytes(StandardCharsets.UTF_8)
        ));

        importer.importSeed("s3://demo-bucket/private/demo-reset/demo-seed.sql");

        assertThat(jdbcTemplate.queryForObject("SELECT name FROM demo_dump_import WHERE id = 1", String.class))
                .isEqualTo("seed");
    }

    @Test
    void shouldRejectInvalidS3Location() {
        assertThatThrownBy(() -> importer.importSeed("s3://demo-bucket"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid S3 demo seed location");
    }

    @Test
    void shouldValidateSeedAvailabilityWithHeadObject() {
        importer.validateSeedAvailable("s3://demo-bucket/private/demo-reset/demo-seed.sql");

        ArgumentCaptor<HeadObjectRequest> requestCaptor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client).headObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("demo-bucket");
        assertThat(requestCaptor.getValue().key()).isEqualTo("private/demo-reset/demo-seed.sql");
    }

    @Test
    void shouldIncludeS3LocationDetails_whenSeedObjectCannotBeLoaded() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(S3Exception.builder()
                .statusCode(404)
                .message("The specified bucket does not exist")
                .build());

        assertThatThrownBy(() -> importer.validateSeedAvailable("s3://missing-bucket/private/demo-reset/demo-seed.sql"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to load demo seed from S3")
                .hasMessageContaining("bucket=missing-bucket")
                .hasMessageContaining("key=private/demo-reset/demo-seed.sql")
                .hasMessageContaining("region=eu-central-1")
                .hasMessageContaining("status=404");
    }
}
