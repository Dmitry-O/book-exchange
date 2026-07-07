package com.example.bookexchange.common.demoaccess;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.i18n.MessageService;
import com.example.bookexchange.common.util.ErrorHelper;
import com.example.bookexchange.common.web.ErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoOriginGuardFilterTest {

    private static final String HEADER_NAME = "X-Origin-Verify";
    private static final String HEADER_VALUE = "cloudfront-secret";

    @Mock
    private MessageService messageService;

    @Mock
    private FilterChain filterChain;

    private AppProperties appProperties;
    private DemoOriginGuardFilter filter;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setRuntimeEnv("demo");
        appProperties.setBaseApiPath("/api/v1");
        appProperties.getDemoOriginGuard().setEnabled(true);
        appProperties.getDemoOriginGuard().setHeaderName(HEADER_NAME);
        appProperties.getDemoOriginGuard().setHeaderValue(HEADER_VALUE);

        filter = new DemoOriginGuardFilter(
                appProperties,
                new ErrorResponseWriter(messageService, new ErrorHelper(), new ObjectMapper().findAndRegisterModules())
        );
    }

    @Test
    void shouldBlockDemoRequestWithoutCloudFrontOriginHeader() throws Exception {
        when(messageService.getMessage(MessageKey.SYSTEM_ACCESS_FORBIDDEN)).thenReturn("Forbidden");
        MockHttpServletRequest request = request("GET", "/api/v1/books/search");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("Forbidden");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldAllowDemoRequestWithCloudFrontOriginHeader() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/books/search");
        request.addHeader(HEADER_NAME, HEADER_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void shouldAllowHealthRequestWithoutCloudFrontOriginHeader() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void shouldAllowRequestsWhenRuntimeIsNotDemo() throws Exception {
        appProperties.setRuntimeEnv("local");
        MockHttpServletRequest request = request("GET", "/api/v1/books/search");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void shouldFailClosedWhenGuardIsEnabledButSecretIsMissing() throws Exception {
        when(messageService.getMessage(MessageKey.SYSTEM_ACCESS_FORBIDDEN)).thenReturn("Forbidden");
        appProperties.getDemoOriginGuard().setHeaderValue(null);
        MockHttpServletRequest request = request("GET", "/api/v1/books/search");
        request.addHeader(HEADER_NAME, HEADER_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        verify(filterChain, never()).doFilter(any(), any());
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
