package com.example.bookexchange.common.demoreset;

import com.example.bookexchange.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.util.StreamUtils.copyToByteArray;

@Testcontainers
@Import(DemoResetServiceIT.S3SeedTestConfig.class)
@TestPropertySource(properties = {
        "app.runtime-env=demo",
        "app.demo-reset.enabled=true",
        "app.demo-reset.seed-s3-key=private/demo-reset/demo-reset-test-seed.sql",
        "app.demo-reset.reindex-search=false",
        "app.demo-reset.clear-mailpit=false",
        "app.storage.s3.prod-bucket=book-exchange-demo-test-images",
        "app.storage.s3.use-test-bucket=false"
})
class DemoResetServiceIT extends IntegrationTestSupport {

    @Autowired
    private DemoResetService demoResetService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private S3Client s3Client;

    @Test
    void shouldImportDemoSeedAfterClearingMutableTables() throws Exception {
        reset(s3Client);
        byte[] seedSql = copyToByteArray(new ClassPathResource("db/demo/demo-reset-test-seed.sql").getInputStream());
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                seedSql
        ));

        assertSuccess(demoResetService.resetDemoEnvironment(), OK);

        assertThat(countRows("app_user")).isEqualTo(4);
        assertThat(countRows("user_roles")).isEqualTo(5);
        assertThat(countRows("book")).isEqualTo(5);
        assertThat(countRows("exchange")).isEqualTo(3);
        assertThat(countRows("report")).isEqualTo(2);
        assertThat(countRows("user_update")).isEqualTo(2);
        assertThat(countRows("city_dictionary")).isGreaterThan(60);
        assertThat(cityName("Berlin", "name_en")).isEqualTo("Berlin");
        assertThat(cityName("Berlin", "name_de")).isEqualTo("Berlin");
    }

    private Integer countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + tableName + "`", Integer.class);
    }

    private String cityName(String canonicalName, String columnName) {
        return jdbcTemplate.queryForObject(
                "SELECT `" + columnName + "` FROM city_dictionary WHERE canonical_name = ?",
                String.class,
                canonicalName
        );
    }

    @TestConfiguration
    static class S3SeedTestConfig {

        @Bean
        @Primary
        S3Client demoResetS3Client() {
            return mock(S3Client.class);
        }
    }
}
