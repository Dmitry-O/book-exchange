package com.example.bookexchange.common.demoaccess;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Success;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

class DemoAccessServiceTest {

    private static final String DEMO_TOKEN = "demo-token";

    private AppProperties appProperties;
    private DemoAccessService demoAccessService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setRuntimeEnv("demo");
        appProperties.getDemoAccess().setTokenHash(sha256Hex(DEMO_TOKEN));
        appProperties.getDemoAccess().setSecureCookie(true);

        demoAccessService = new DemoAccessService(appProperties);
    }

    @Test
    void shouldIssueHttpOnlyCookie_whenDemoTokenIsValid() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Success<DemoAccessVerificationDTO> success = assertSuccess(
                demoAccessService.verifyAccessToken(DEMO_TOKEN, response),
                HttpStatus.OK
        );

        assertThat(success.body().enabled()).isTrue();
        assertThat(success.body().expiresAt()).isNotNull();
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("BE_DEMO_ACCESS=")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }

    @Test
    void shouldRejectInvalidDemoToken() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFailure(
                demoAccessService.verifyAccessToken("wrong-token", response),
                MessageKey.SYSTEM_INVALID_TOKEN,
                HttpStatus.UNAUTHORIZED
        );
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void shouldNotRequireDemoAccessOutsideDemoRuntime() {
        appProperties.setRuntimeEnv("local");
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(demoAccessService.isDemoGateEnabled()).isFalse();
        assertThat(demoAccessService.hasValidAccessCookie(request)).isTrue();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
