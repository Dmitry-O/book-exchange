package com.example.bookexchange.user.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.auth.api.AuthPaths;
import com.example.bookexchange.auth.dto.RefreshTokenDTO;
import com.example.bookexchange.auth.repository.RefreshTokenRepository;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.user.dto.UserCreateDTO;
import com.example.bookexchange.user.dto.UserResetPasswordDTO;
import com.example.bookexchange.user.dto.UserUpdateDTO;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.support.fixture.BookFixtureSupport;
import com.example.bookexchange.support.fixture.UserFixtureSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class UserControllerIT extends IntegrationTestSupport {

    @Autowired
    UserRepository userRepository;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

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
    void shouldReturnCurrentUser_whenUserGetsOwnProfile() throws Exception {
        UserFixture fixture = createUserFixture(200);

        MvcResult mvcResult = mockMvc.perform(get(UserPaths.USER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        JsonNode data = body.path("data");

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(data.path("id").asLong()).isEqualTo(fixture.user().getId());
        assertThat(data.path("email").asText()).isEqualTo(fixture.user().getEmail());
        assertThat(data.path("nickname").asText()).isEqualTo(fixture.user().getNickname());
        assertThat(data.path("locale").asText()).isEqualTo(fixture.user().getLocale());
        assertThat(eTagVersion(mvcResult)).isEqualTo(fixture.user().getVersion());
    }

    @Test
    void shouldUpdateUser_whenPayloadIsValid() throws Exception {
        UserFixture fixture = createUserFixture(201);
        UserUpdateDTO dto = UserUpdateDTO.builder()
                .nickname("updated_user_201")
                .photoBase64(validBase64("updated-user-photo"))
                .locale("de")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(UserPaths.USER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(fixture.user().getVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        clearPersistenceContext();

        User updatedUser = userRepository.findById(fixture.user().getId()).orElseThrow();
        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("id").asLong()).isEqualTo(updatedUser.getId());
        assertThat(body.path("data").path("nickname").asText()).isEqualTo(dto.getNickname());
        assertThat(body.path("data").path("locale").asText()).isEqualTo(dto.getLocale());
        assertThat(updatedUser.getNickname()).isEqualTo(dto.getNickname());
        assertThat(updatedUser.getPhotoBase64()).isEqualTo(dto.getPhotoBase64());
        assertThat(updatedUser.getLocale()).isEqualTo(dto.getLocale());
        assertThat(mvcResult.getResponse().getHeader(HttpHeaders.ETAG)).isNotBlank();
    }

    @Test
    void shouldReturnBadRequest_whenUpdateUserPayloadIsInvalid() throws Exception {
        UserFixture fixture = createUserFixture(202);
        UserUpdateDTO dto = UserUpdateDTO.builder()
                .nickname("ab")
                .photoBase64(validBase64("user-photo"))
                .locale("en")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(UserPaths.USER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(fixture.user().getVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertValidationErrorResponse(responseBody(mvcResult), UserPaths.USER_PATH);
    }

    @Test
    void shouldReturnConflict_whenUpdateUserNicknameAlreadyExists() throws Exception {
        User existingUser = userUtil.createUser(203);
        UserFixture fixture = createUserFixture(204);
        UserUpdateDTO dto = UserUpdateDTO.builder()
                .nickname(existingUser.getNickname())
                .photoBase64(validBase64("user-photo"))
                .locale("en")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(UserPaths.USER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(fixture.user().getVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 409, MessageKey.AUTH_NICKNAME_ALREADY_EXISTS, UserPaths.USER_PATH);
    }

    @Test
    void shouldReturnConflict_whenUpdateUserVersionIsStale() throws Exception {
        UserFixture fixture = createUserFixture(205);
        UserUpdateDTO dto = UserUpdateDTO.builder()
                .nickname("updated_user_205")
                .photoBase64(validBase64("user-photo"))
                .locale("en")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(UserPaths.USER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 409, MessageKey.SYSTEM_OPTIMISTIC_LOCK, UserPaths.USER_PATH);
    }

    @Test
    void shouldReturnBadRequest_whenUpdateUserIfMatchHeaderIsInvalid() throws Exception {
        UserFixture fixture = createUserFixture(2051);
        UserUpdateDTO dto = UserUpdateDTO.builder()
                .nickname("updated_user_invalid_etag")
                .photoBase64(validBase64("user-photo"))
                .locale("en")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(UserPaths.USER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .header(HttpHeaders.IF_MATCH, "\"invalid\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertValidationErrorResponse(responseBody(mvcResult), UserPaths.USER_PATH);
    }

    @Test
    void shouldResetPassword_whenCurrentPasswordIsCorrect() throws Exception {
        UserFixture fixture = createUserFixture(206);
        UserResetPasswordDTO dto = UserResetPasswordDTO.builder()
                .currentPassword(fixture.credentials().getPassword())
                .newPassword("NewPassword1!")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(UserPaths.USER_PATH_RESET_PASSWORD)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(fixture.user().getVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        User updatedUser = userRepository.findById(fixture.user().getId()).orElseThrow();
        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").isNull()).isTrue();
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(passwordEncoder.matches(dto.getNewPassword(), updatedUser.getPassword())).isTrue();
    }

    @Test
    void shouldReturnBadRequest_whenResetPasswordCurrentPasswordIsWrong() throws Exception {
        UserFixture fixture = createUserFixture(207);
        UserResetPasswordDTO dto = UserResetPasswordDTO.builder()
                .currentPassword("WrongPassword1!")
                .newPassword("NewPassword1!")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(UserPaths.USER_PATH_RESET_PASSWORD)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(fixture.user().getVersion()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.AUTH_WRONG_ACTUAL_PASSWORD,
                UserPaths.USER_PATH_RESET_PASSWORD
        );
    }

    @Test
    void shouldLogoutUser_whenRefreshTokenIsValid() throws Exception {
        UserFixture fixture = createUserFixture(208);
        String refreshToken = loginAndGetRefreshToken(fixture);
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setToken(refreshToken);

        MvcResult mvcResult = mockMvc.perform(patch(UserPaths.USER_PATH_LOGOUT)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").isNull()).isTrue();
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    void shouldDeleteUser_whenVersionIsCurrent() throws Exception {
        UserFixture fixture = createUserFixture(209);
        Long bookId = bookUtil.createBook(fixture.user().getId(), 209);

        MvcResult mvcResult = mockMvc.perform(delete(UserPaths.USER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .header(HttpHeaders.IF_MATCH, ifMatch(fixture.user().getVersion()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        User deletedUser = userRepository.findById(fixture.user().getId()).orElseThrow();
        Book deletedBook = bookRepository.findById(bookId).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").isNull()).isTrue();
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(deletedUser.getDeletedAt()).isNotNull();
        assertThat(deletedUser.getEmail()).startsWith("anonymized-" + fixture.user().getId());
        assertThat(deletedBook.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldReturnConflict_whenDeleteUserVersionIsStale() throws Exception {
        UserFixture fixture = createUserFixture(210);

        MvcResult mvcResult = mockMvc.perform(delete(UserPaths.USER_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(fixture.user()))
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 409, MessageKey.SYSTEM_OPTIMISTIC_LOCK, UserPaths.USER_PATH);
    }

    private UserFixture createUserFixture(int userNumber) {
        UserCreateDTO credentials = userUtil.buildUserCreateDTO(userNumber);
        User user = userUtil.createUser(userNumber);

        return new UserFixture(user, credentials);
    }

    private String loginAndGetRefreshToken(UserFixture fixture) throws Exception {
        MvcResult loginResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(
                                fixture.credentials().getEmail(),
                                fixture.credentials().getPassword()
                        )))
                .andExpect(status().isOk())
                .andReturn();

        return responseBody(loginResult).path("data").path("refreshToken").asText();
    }

    private record UserFixture(User user, UserCreateDTO credentials) {
    }
}
