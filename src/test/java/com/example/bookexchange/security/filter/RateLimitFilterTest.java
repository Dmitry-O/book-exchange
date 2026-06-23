package com.example.bookexchange.security.filter;

import com.example.bookexchange.auth.api.AuthPaths;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.demoaccess.DemoAccessPaths;
import com.example.bookexchange.common.demoaccounts.DemoAccountsPaths;
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

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private MessageService messageService;

    @Mock
    private AuditService auditService;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        ErrorResponseWriter errorResponseWriter = new ErrorResponseWriter(
                messageService,
                new ErrorHelper(),
                new ObjectMapper().findAndRegisterModules()
        );
        rateLimitFilter = new RateLimitFilter(errorResponseWriter, auditService);
        when(messageService.getMessage(MessageKey.SYSTEM_TOO_MANY_REQUESTS))
                .thenReturn("Too many requests");
    }

    @Test
    void shouldRateLimitTokenValidationSeparatelyFromOtherAuthActions() throws Exception {
        for (int requestNumber = 0; requestNumber < 10; requestNumber++) {
            rateLimitFilter.doFilterInternal(
                    request(AuthPaths.AUTH_PATH_VALIDATE_TOKEN),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }

        rateLimitFilter.doFilterInternal(
                request(AuthPaths.AUTH_PATH_RESET_PASSWORD),
                new MockHttpServletResponse(),
                filterChain
        );
        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(
                request(AuthPaths.AUTH_PATH_VALIDATE_TOKEN),
                limitedResponse,
                filterChain
        );

        verify(filterChain, times(11)).doFilter(any(), any());
        assertThat(limitedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(limitedResponse.getHeader("X-Rate-Limit-Retry-After")).isNotBlank();
    }

    @Test
    void shouldRateLimitDemoAccessVerificationSeparately() throws Exception {
        for (int requestNumber = 0; requestNumber < 10; requestNumber++) {
            rateLimitFilter.doFilterInternal(
                    request(DemoAccessPaths.DEMO_ACCESS_VERIFY_PATH),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }

        rateLimitFilter.doFilterInternal(
                request(AuthPaths.AUTH_PATH_LOGIN),
                new MockHttpServletResponse(),
                filterChain
        );
        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(
                request(DemoAccessPaths.DEMO_ACCESS_VERIFY_PATH),
                limitedResponse,
                filterChain
        );

        verify(filterChain, times(11)).doFilter(any(), any());
        assertThat(limitedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(limitedResponse.getHeader("X-Rate-Limit-Retry-After")).isNotBlank();
    }

    @Test
    void shouldRateLimitDemoAccountsSeparately() throws Exception {
        for (int requestNumber = 0; requestNumber < 30; requestNumber++) {
            rateLimitFilter.doFilterInternal(
                    request(DemoAccountsPaths.DEMO_ACCOUNTS_PATH),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }

        rateLimitFilter.doFilterInternal(
                request(AuthPaths.AUTH_PATH_LOGIN),
                new MockHttpServletResponse(),
                filterChain
        );
        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(
                request(DemoAccountsPaths.DEMO_ACCOUNTS_PATH),
                limitedResponse,
                filterChain
        );

        verify(filterChain, times(31)).doFilter(any(), any());
        assertThat(limitedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(limitedResponse.getHeader("X-Rate-Limit-Retry-After")).isNotBlank();
    }

    @Test
    void shouldRateLimitSwaggerRequests() throws Exception {
        for (int requestNumber = 0; requestNumber < 60; requestNumber++) {
            rateLimitFilter.doFilterInternal(
                    request("/api/v1/swagger-ui/index.html"),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }

        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        rateLimitFilter.doFilterInternal(
                request("/api/v1/swagger-ui/index.html"),
                limitedResponse,
                filterChain
        );

        verify(filterChain, times(60)).doFilter(any(), any());
        assertThat(limitedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
