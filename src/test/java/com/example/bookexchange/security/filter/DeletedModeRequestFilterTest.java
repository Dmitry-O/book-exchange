package com.example.bookexchange.security.filter;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.security.context.RequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DeletedModeRequestFilterTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void shouldEnableIncludeDeletedForAdminRequests() throws ServletException, IOException {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseApiPath("/api/v1");

        DeletedModeRequestFilter filter = new DeletedModeRequestFilter(appProperties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean includeDeletedInsideChain = new AtomicBoolean(false);

        FilterChain filterChain = (req, res) -> includeDeletedInsideChain.set(RequestContextHolder.isIncludeDeleted());

        filter.doFilter(request, response, filterChain);

        assertThat(includeDeletedInsideChain.get()).isTrue();
        assertThat(RequestContextHolder.isIncludeDeleted()).isFalse();
    }

    @Test
    void shouldKeepIncludeDeletedDisabledForNonAdminRequests() throws ServletException, IOException {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseApiPath("/api/v1");

        DeletedModeRequestFilter filter = new DeletedModeRequestFilter(appProperties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books/search");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean includeDeletedInsideChain = new AtomicBoolean(true);

        FilterChain filterChain = (req, res) -> includeDeletedInsideChain.set(RequestContextHolder.isIncludeDeleted());

        filter.doFilter(request, response, filterChain);

        assertThat(includeDeletedInsideChain.get()).isFalse();
        assertThat(RequestContextHolder.isIncludeDeleted()).isFalse();
    }
}
