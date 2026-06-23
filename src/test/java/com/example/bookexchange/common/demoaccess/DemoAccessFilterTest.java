package com.example.bookexchange.common.demoaccess;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.i18n.MessageService;
import com.example.bookexchange.common.util.ErrorHelper;
import com.example.bookexchange.common.web.ErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoAccessFilterTest {

    private static final String DEMO_TOKEN = "demo-token";

    @Mock
    private MessageService messageService;

    @Mock
    private FilterChain filterChain;

    private AppProperties appProperties;
    private DemoAccessService demoAccessService;
    private DemoAccessFilter demoAccessFilter;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setRuntimeEnv("demo");
        appProperties.getDemoAccess().setTokenHash(sha256Hex(DEMO_TOKEN));
        appProperties.getDemoAccess().setSecureCookie(false);

        demoAccessService = new DemoAccessService(appProperties);
        demoAccessFilter = new DemoAccessFilter(
                demoAccessService,
                appProperties,
                new ErrorResponseWriter(messageService, new ErrorHelper(), new ObjectMapper().findAndRegisterModules())
        );
    }

    @Test
    void shouldBlockSwaggerWithoutDemoAccessCookie() throws Exception {
        when(messageService.getMessage(MessageKey.SYSTEM_DEMO_ACCESS_REQUIRED))
                .thenReturn("Demo access is required");
        MockHttpServletResponse response = new MockHttpServletResponse();

        demoAccessFilter.doFilterInternal(
                request("GET", "/api/v1/swagger-ui/index.html"),
                response,
                filterChain
        );

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldAllowSwaggerWithDemoAccessCookie() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/swagger-ui/index.html");
        request.setCookies(accessCookie());
        MockHttpServletResponse response = new MockHttpServletResponse();

        demoAccessFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowDemoAccessVerificationWithoutCookie() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/demo/access/verify");
        MockHttpServletResponse response = new MockHttpServletResponse();

        demoAccessFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    private Cookie accessCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        demoAccessService.verifyAccessToken(DEMO_TOKEN, response);
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        String cookieValue = setCookie.substring("BE_DEMO_ACCESS=".length(), setCookie.indexOf(";"));
        return new Cookie("BE_DEMO_ACCESS", cookieValue);
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        return request;
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
