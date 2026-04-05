package com.example.bookexchange.security;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.admin.api.AdminPaths;
import com.example.bookexchange.book.api.BookPaths;
import com.example.bookexchange.common.i18n.MessageKey;
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
    void protectedEndpointRequiresAuthentication() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(UserPaths.USER_PATH))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 401, MessageKey.SYSTEM_INVALID_TOKEN, UserPaths.USER_PATH);
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(UserPaths.USER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 401, MessageKey.SYSTEM_INVALID_TOKEN, UserPaths.USER_PATH);
    }

    @Test
    void publicSearchEndpointIsAccessibleWithoutAuthentication() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(BookPaths.BOOK_PATH_SEARCH)
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString()))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(responseBody(mvcResult).path("success").asBoolean()).isTrue();
    }

    @Test
    void adminEndpointIsForbiddenForRegularUser() throws Exception {
        User user = userUtil.createUser(300);

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
    void adminEndpointIsAccessibleForAdmin() throws Exception {
        User adminUser = userUtil.createUser(301);
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
