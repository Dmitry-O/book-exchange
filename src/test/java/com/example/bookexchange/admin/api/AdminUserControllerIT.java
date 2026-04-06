package com.example.bookexchange.admin.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.admin.dto.BanUserDTO;
import com.example.bookexchange.auth.model.TokenType;
import com.example.bookexchange.auth.repository.RefreshTokenRepository;
import com.example.bookexchange.auth.repository.VerificationTokenRepository;
import com.example.bookexchange.auth.service.RefreshTokenService;
import com.example.bookexchange.auth.service.VerificationTokenService;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.support.fixture.BookFixtureSupport;
import com.example.bookexchange.support.PageTestDefaults;
import com.example.bookexchange.support.FixtureNumbers;
import com.example.bookexchange.support.fixture.UserFixtureSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class AdminUserControllerIT extends IntegrationTestSupport {

    @Autowired
    UserRepository userRepository;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    VerificationTokenRepository verificationTokenRepository;

    @Autowired
    RefreshTokenService refreshTokenService;

    @Autowired
    VerificationTokenService verificationTokenService;

    @Autowired
    UserFixtureSupport userUtil;

    @Autowired
    BookFixtureSupport bookUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldGiveAdminRights_whenSuperAdminPromotesUser() throws Exception {
        User superAdmin = userUtil.createSuperAdmin(FixtureNumbers.adminUser(1));
        User targetUser = userUtil.createUser(FixtureNumbers.adminUser(2));

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_USERS_ID_GIVE_ADMIN_RIGHTS, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(superAdmin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        User updatedUser = userRepository.findById(targetUser.getId()).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(targetUser.getId());
        assertThat(body.path("data").path("roles").toString()).contains(UserRole.ADMIN.name());
        assertThat(updatedUser.getRoles()).contains(UserRole.ADMIN);
    }

    @Test
    void shouldReturnForbidden_whenRegularAdminTriesToPromoteUser() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminUser(3));
        User targetUser = userUtil.createUser(FixtureNumbers.adminUser(4));

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_USERS_ID_GIVE_ADMIN_RIGHTS, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                403,
                MessageKey.SYSTEM_ACCESS_FORBIDDEN,
                AdminPaths.ADMIN_PATH_USERS + "/" + targetUser.getId() + "/make-admin"
        );
    }

    @Test
    void shouldReturnConflict_whenSuperAdminPromotesExistingAdmin() throws Exception {
        User superAdmin = userUtil.createSuperAdmin(FixtureNumbers.adminUser(21));
        User targetAdmin = userUtil.createAdmin(FixtureNumbers.adminUser(22));

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_USERS_ID_GIVE_ADMIN_RIGHTS, targetAdmin.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(superAdmin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.ADMIN_USER_ALREADY_ADMIN,
                AdminPaths.ADMIN_PATH_USERS + "/" + targetAdmin.getId() + "/make-admin"
        );
    }

    @Test
    void shouldRevokeAdminRights_whenSuperAdminDemotesAdmin() throws Exception {
        User superAdmin = userUtil.createSuperAdmin(FixtureNumbers.adminUser(5));
        User targetAdmin = userUtil.createAdmin(FixtureNumbers.adminUser(6));

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_USERS_ID_REVOKE_ADMIN_RIGHTS, targetAdmin.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(superAdmin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        User updatedUser = userRepository.findById(targetAdmin.getId()).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("roles").toString()).doesNotContain(UserRole.ADMIN.name());
        assertThat(updatedUser.getRoles()).doesNotContain(UserRole.ADMIN);
    }

    @Test
    void shouldReturnBadRequest_whenSuperAdminDemotesNonAdmin() throws Exception {
        User superAdmin = userUtil.createSuperAdmin(FixtureNumbers.adminUser(23));
        User targetUser = userUtil.createUser(FixtureNumbers.adminUser(24));

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_USERS_ID_REVOKE_ADMIN_RIGHTS, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(superAdmin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.ADMIN_USER_NOT_ADMIN,
                AdminPaths.ADMIN_PATH_USERS + "/" + targetUser.getId() + "/remove-admin"
        );
    }

    @Test
    void shouldReturnUsers_whenAdminGetsUsers() throws Exception {
        User superAdmin = userUtil.createSuperAdmin(FixtureNumbers.adminUser(7));
        User targetAdmin = userUtil.createAdmin(FixtureNumbers.adminUser(8));
        userUtil.createUser(FixtureNumbers.adminUser(9));

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_USERS)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(superAdmin))
                        .queryParam("pageIndex", PageTestDefaults.PAGE_INDEX.toString())
                        .queryParam("pageSize", PageTestDefaults.PAGE_SIZE.toString())
                        .queryParam("searchText", targetAdmin.getEmail())
                        .queryParam("roles", UserRole.ADMIN.name())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode content = body.path("data").path("content");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(content.get(0).path("id").asLong()).isEqualTo(targetAdmin.getId());
        assertThat(content.get(0).path("roles").toString()).contains(UserRole.ADMIN.name());
    }

    @Test
    void shouldReturnUser_whenAdminGetsUserById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminUser(10));
        User targetUser = userUtil.createUser(FixtureNumbers.adminUser(11));

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_USERS_ID, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(targetUser.getId());
        assertThat(body.path("data").path("email").asText()).isEqualTo(targetUser.getEmail());
        assertThat(body.path("data").path("nickname").asText()).isEqualTo(targetUser.getNickname());
    }

    @Test
    void shouldReturnNotFound_whenAdminGetsMissingUserById() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminUser(25));

        MvcResult mvcResult = mockMvc.perform(get(AdminPaths.ADMIN_PATH_USERS_ID, Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.ADMIN_USER_NOT_FOUND,
                AdminPaths.ADMIN_PATH_USERS + "/" + Long.MAX_VALUE
        );
    }

    @Test
    void shouldBanUserTemporarily_whenAdminSubmitsTemporaryBan() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminUser(12));
        User targetUser = userUtil.createUser(FixtureNumbers.adminUser(13));
        String refreshToken = refreshTokenService.createToken(targetUser);
        BanUserDTO dto = BanUserDTO.builder()
                .bannedUntil(OffsetDateTime.parse("2026-05-01T12:00:00Z"))
                .banReason("Repeated spam offers")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_USERS_ID_BAN, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, ifMatch(targetUser.getVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        User bannedUser = userRepository.findById(targetUser.getId()).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("bannedPermanently").asBoolean()).isFalse();
        assertThat(body.path("data").path("banReason").asText()).isEqualTo(dto.getBanReason());
        assertThat(bannedUser.getBannedUntil()).isNotNull();
        assertThat(bannedUser.getBanReason()).isEqualTo(dto.getBanReason());
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    void shouldReturnBadRequest_whenAdminTriesToBanThemself() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminUser(14));
        BanUserDTO dto = BanUserDTO.builder()
                .bannedPermanently(true)
                .banReason("Should not happen")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_USERS_ID_BAN, admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, ifMatch(admin.getVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.ADMIN_CANT_BAN_YOURSELF,
                AdminPaths.ADMIN_PATH_USERS + "/" + admin.getId() + "/ban"
        );
    }

    @Test
    void shouldReturnBadRequest_whenAdminBanPayloadFailsValidation() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminUser(26));
        User targetUser = userUtil.createUser(FixtureNumbers.adminUser(27));
        BanUserDTO dto = BanUserDTO.builder()
                .banReason("Missing ban window")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_USERS_ID_BAN, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, ifMatch(targetUser.getVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertValidationErrorResponse(
                responseBody(mvcResult),
                AdminPaths.ADMIN_PATH_USERS + "/" + targetUser.getId() + "/ban"
        );
    }

    @Test
    void shouldUnbanUser_whenAdminRemovesBan() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminUser(15));
        User targetUser = userUtil.createUser(FixtureNumbers.adminUser(16));
        targetUser.setBannedUntil(OffsetDateTime.parse("2026-05-01T12:00:00Z").toInstant());
        targetUser.setBanReason("Old ban");
        userRepository.save(targetUser);

        clearPersistenceContext();

        User persistedUser = userRepository.findById(targetUser.getId()).orElseThrow();

        MvcResult mvcResult = mockMvc.perform(patch(AdminPaths.ADMIN_PATH_USERS_ID_UNBAN, persistedUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, ifMatch(persistedUser.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        User unbannedUser = userRepository.findById(targetUser.getId()).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("banReason").isNull()).isTrue();
        assertThat(unbannedUser.getBannedUntil()).isNull();
        assertThat(unbannedUser.isBannedPermanently()).isFalse();
        assertThat(unbannedUser.getBanReason()).isNull();
    }

    @Test
    void shouldDeleteUser_whenAdminDeletesUser() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminUser(17));
        User targetUser = userUtil.createUser(FixtureNumbers.adminUser(18));
        Long targetBookId = bookUtil.createBook(targetUser.getId(), FixtureNumbers.adminUser(18));
        String refreshToken = refreshTokenService.createToken(targetUser);
        verificationTokenService.createToken(targetUser, TokenType.DELETE_ACCOUNT);
        String verificationToken = verificationTokenRepository.findByUserAndType(targetUser, TokenType.DELETE_ACCOUNT)
                .orElseThrow()
                .getToken();

        MvcResult mvcResult = mockMvc.perform(delete(AdminPaths.ADMIN_PATH_USERS_ID, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, ifMatch(targetUser.getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        User deletedUser = userRepository.findById(targetUser.getId()).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(targetUser.getId());
        assertThat(deletedUser.getDeletedAt()).isNotNull();
        assertThat(deletedUser.getEmail()).startsWith("anonymized-" + targetUser.getId());
        assertThat(bookRepository.findById(targetBookId).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
        assertThat(verificationTokenRepository.findByToken(verificationToken)).isEmpty();
    }

    @Test
    void shouldReturnConflict_whenAdminDeletesUserWithStaleVersion() throws Exception {
        User admin = userUtil.createAdmin(FixtureNumbers.adminUser(19));
        User targetUser = userUtil.createUser(FixtureNumbers.adminUser(20));

        MvcResult mvcResult = mockMvc.perform(delete(AdminPaths.ADMIN_PATH_USERS_ID, targetUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(admin))
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                409,
                MessageKey.SYSTEM_OPTIMISTIC_LOCK,
                AdminPaths.ADMIN_PATH_USERS + "/" + targetUser.getId()
        );
    }
}
