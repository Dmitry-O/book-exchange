package com.example.bookexchange.common.demoreset;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.i18n.MessageService;
import com.example.bookexchange.common.util.ErrorHelper;
import com.example.bookexchange.common.web.ErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoMaintenanceFilterTest {

    @Mock
    private MessageService messageService;

    @Mock
    private FilterChain filterChain;

    private AppProperties appProperties;
    private DemoMaintenanceService demoMaintenanceService;
    private DemoMaintenanceFilter demoMaintenanceFilter;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setRuntimeEnv("demo");
        appProperties.setBaseApiPath("/api/v1");
        demoMaintenanceService = new DemoMaintenanceService();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ErrorResponseWriter errorResponseWriter = new ErrorResponseWriter(messageService, new ErrorHelper(), objectMapper);
        demoMaintenanceFilter = new DemoMaintenanceFilter(appProperties, demoMaintenanceService, errorResponseWriter);
    }

    @Test
    void shouldBlockApiRequests_whenDemoMaintenanceModeIsEnabled() throws Exception {
        when(messageService.getMessage(MessageKey.SYSTEM_MAINTENANCE)).thenReturn("maintenance");
        demoMaintenanceService.enable();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books/search");
        MockHttpServletResponse response = new MockHttpServletResponse();

        demoMaintenanceFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("maintenance");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldAllowHealthRequests_whenDemoMaintenanceModeIsEnabled() throws Exception {
        demoMaintenanceService.enable();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        demoMaintenanceFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldAllowApiRequests_whenRuntimeIsNotDemo() throws Exception {
        appProperties.setRuntimeEnv("local");
        demoMaintenanceService.enable();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books/search");
        MockHttpServletResponse response = new MockHttpServletResponse();

        demoMaintenanceFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
