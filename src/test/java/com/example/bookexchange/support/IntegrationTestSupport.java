package com.example.bookexchange.support;

import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.config.TestUserConfig;
import com.example.bookexchange.security.auth.JwtService;
import com.example.bookexchange.user.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MySQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestUserConfig.class)
@ActiveProfiles("localmysql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestSupport {

    protected static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected EntityManager entityManager;

    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:9").withReuse(true);

    static {
        mySQLContainer.start();
    }

    @DynamicPropertySource
    static void mysqlProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> 2525);
        registry.add("spring.mail.username", () -> "test");
        registry.add("spring.mail.password", () -> "test");
        registry.add("app.jwt-secret-key", () -> "test-jwt-secret-key-which-is-at-least-32-bytes-long");
        registry.add("app.email-sent-from", () -> "noreply@test.com");
        registry.add("spring.mvc.servlet.path", () -> "/");
        registry.add("app.base-api-path", () -> "/");
    }

    protected MockMvc buildMockMvc() {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    protected String bearerToken(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    protected JsonNode responseBody(MvcResult mvcResult) throws Exception {
        return objectMapper.readTree(mvcResult.getResponse().getContentAsString());
    }

    protected String ifMatch(Long version) {
        return "\"" + version + "\"";
    }

    protected long eTagVersion(MvcResult mvcResult) {
        String eTag = mvcResult.getResponse().getHeader(HttpHeaders.ETAG);

        return Long.parseLong(eTag.replace("\"", ""));
    }

    protected void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    protected String validBase64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    protected void assertErrorResponse(JsonNode body, int status, String errorType, String path) {
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("data").isNull()).isTrue();
        assertThat(body.path("message").isNull()).isTrue();
        assertThat(body.path("error").path("status").asInt()).isEqualTo(status);
        assertThat(body.path("error").path("error").asText()).isEqualTo(errorType);
        assertThat(body.path("error").path("message").asText()).isNotBlank();
        assertThat(body.path("error").path("path").asText()).isEqualTo(path);
        assertThat(body.path("error").path("requestId").asText()).isNotBlank();
    }

    protected void assertErrorResponse(JsonNode body, int status, MessageKey errorType, String path) {
        assertErrorResponse(body, status, errorType.name(), path);
    }

    protected void assertValidationErrorResponse(JsonNode body, String path) {
        assertErrorResponse(body, 400, VALIDATION_ERROR, path);
    }

    protected void assertHasVersion(JsonNode node) {
        assertThat(node.hasNonNull("version")).isTrue();
        assertThat(node.get("version").isIntegralNumber()).isTrue();
        assertThat(node.get("version").longValue()).isGreaterThanOrEqualTo(0L);
    }

    protected void assertVersion(JsonNode node, long expectedVersion) {
        assertThat(node.hasNonNull("version")).isTrue();
        assertThat(node.get("version").isIntegralNumber()).isTrue();
        assertThat(node.get("version").longValue()).isEqualTo(expectedVersion);
    }
}
