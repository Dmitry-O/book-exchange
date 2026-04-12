package com.example.bookexchange.security;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.admin.api.AdminPaths;
import com.example.bookexchange.book.api.BookPaths;
import com.example.bookexchange.common.api.MetadataPaths;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.support.FixtureNumbers;
import com.example.bookexchange.user.api.UserPaths;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.support.PageTestDefaults;
import com.example.bookexchange.support.fixture.UserFixtureSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
class SecurityIT extends IntegrationTestSupport {

    @Autowired
    UserFixtureSupport userUtil;

    @Autowired
    UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldRequireAuthentication_whenProtectedEndpointIsCalledWithoutToken() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(UserPaths.USER_PATH))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 401, MessageKey.SYSTEM_INVALID_TOKEN, UserPaths.USER_PATH);
    }

    @Test
    void shouldRejectInvalidToken_whenProtectedEndpointIsCalled() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(UserPaths.USER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 401, MessageKey.SYSTEM_INVALID_TOKEN, UserPaths.USER_PATH);
    }

    @Test
    void shouldAllowAnonymousAccess_whenPublicSearchEndpointIsCalled() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_SEARCH)
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString()))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(responseBody(mvcResult).path("success").asBoolean()).isTrue();
    }

    @Test
    void shouldAllowAnonymousAccess_whenPublicMetadataEndpointIsCalled() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(MetadataPaths.METADATA_PATH))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("locales").isArray()).isTrue();
        assertThat(body.path("data").path("reportReasons").isArray()).isTrue();
        assertThat(body.path("data").path("reportStatuses").isArray()).isTrue();
        assertThat(body.path("data").path("exchangeStatuses").isArray()).isTrue();
        assertThat(body.path("data").path("bookTypes").isArray()).isTrue();
        assertThat(body.path("data").path("userTypes").isArray()).isTrue();
        assertThat(body.path("data").path("roles").isArray()).isTrue();
        assertThat(body.path("data").path("bookSortFields").isArray()).isTrue();
    }

    @Test
    void shouldReturnCorsHeaders_whenPreflightRequestMatchesAllowedOrigin() throws Exception {
        mockMvc.perform(options(UserPaths.USER_PATH)
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,If-Match"))
                .andExpect(status().isOk())
                .andExpect(result ->
                        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                                .isEqualTo("http://localhost:5173"));
    }

    @Test
    void shouldReturnForbidden_whenRegularUserCallsAdminEndpoint() throws Exception {
        User user = userUtil.createUser(FixtureNumbers.security(300));

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_USERS)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(user))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString()))
                .andExpect(status().isForbidden())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                403,
                MessageKey.SYSTEM_ACCESS_FORBIDDEN,
                AdminPaths.ADMIN_PATH_USERS
        );
    }

    @Test
    void shouldAllowAccess_whenAdminCallsAdminEndpoint() throws Exception {
        User adminUser = userUtil.createUser(FixtureNumbers.security(301));
        adminUser.addRole(UserRole.ADMIN);
        userRepository.save(adminUser);

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_USERS)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminUser))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("content").isArray()).isTrue();
    }
}
