package com.example.bookexchange.auth.api;

import com.example.bookexchange.support.IntegrationTestSupport;
import com.example.bookexchange.auth.dto.AuthRefreshTokenDTO;
import com.example.bookexchange.auth.model.RefreshToken;
import com.example.bookexchange.auth.model.TokenType;
import com.example.bookexchange.auth.model.VerificationToken;
import com.example.bookexchange.auth.repository.RefreshTokenRepository;
import com.example.bookexchange.auth.repository.VerificationTokenRepository;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.support.FixtureNumbers;
import com.example.bookexchange.support.TestReportStrings;
import com.example.bookexchange.user.dto.UserCreateDTO;
import com.example.bookexchange.user.dto.UserForgotPasswordDTO;
import com.example.bookexchange.user.dto.UserInitiateDeleteAccountDTO;
import com.example.bookexchange.user.dto.UserResendEmailConfirmationDTO;
import com.example.bookexchange.user.dto.UserResetForgottenPasswordDTO;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.support.fixture.UserFixtureSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@Transactional
@Rollback
class AuthControllerIT extends IntegrationTestSupport {

    @Autowired
    UserRepository userRepository;

    @Autowired
    VerificationTokenRepository verificationTokenRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserFixtureSupport userUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc();
    }

    @Test
    void shouldRegisterUser_whenPayloadIsValid() throws Exception {
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(FixtureNumbers.auth(1));

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateDTO)))
                .andExpect(status().isOk())
                .andReturn();

        User savedUser = userRepository.findByEmail(userCreateDTO.getEmail()).orElseThrow();
        VerificationToken verificationToken = verificationTokenRepository
                .findByUserAndType(savedUser, TokenType.CONFIRM_EMAIL)
                .orElseThrow();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").isNull()).isTrue();
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(savedUser.isEmailVerified()).isFalse();
        assertThat(verificationToken.getToken()).isNotBlank();
    }

    @Test
    void shouldReturnBadRequest_whenRegisterPayloadIsInvalid() throws Exception {
        UserCreateDTO invalidDto = UserCreateDTO.builder().build();

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertValidationErrorResponse(responseBody(mvcResult), AuthPaths.AUTH_PATH_REGISTER);
    }

    @Test
    void shouldReturnConflict_whenRegisterEmailAlreadyExists() throws Exception {
        User existingUser = userUtil.createUser(FixtureNumbers.auth(2));
        UserCreateDTO duplicateDto = userUtil.buildUserCreateDTO(FixtureNumbers.auth(2));

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(existingUser.getEmail()).isEqualTo(duplicateDto.getEmail());
        assertErrorResponse(responseBody(mvcResult), 409, MessageKey.AUTH_EMAIL_ALREADY_EXISTS, AuthPaths.AUTH_PATH_REGISTER);
    }

    @Test
    void shouldReturnForbidden_whenRegisterEmailBelongsToUnverifiedAccount() throws Exception {
        int slot = FixtureNumbers.auth(3);
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(slot);
        userUtil.createUser(slot, false);

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateDTO)))
                .andExpect(status().isForbidden())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                403,
                MessageKey.EMAIL_VERIFY_ACCOUNT,
                AuthPaths.AUTH_PATH_REGISTER
        );
    }

    @Test
    void shouldLoginUser_whenCredentialsAreValid() throws Exception {
        User user = userUtil.createUser(FixtureNumbers.auth(4));
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(FixtureNumbers.auth(4));

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(userCreateDTO.getEmail(), userCreateDTO.getPassword())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);
        String refreshToken = body.path("data").path("refreshToken").asText();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").path("accessToken").asText()).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isPresent();
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void shouldReturnForbidden_whenLoginUserEmailIsNotVerified() throws Exception {
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(FixtureNumbers.auth(5));
        userUtil.createUser(FixtureNumbers.auth(5), false);

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(userCreateDTO.getEmail(), userCreateDTO.getPassword())))
                .andExpect(status().isForbidden())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                403,
                MessageKey.AUTH_ACCOUNT_NOT_VERIFIED,
                AuthPaths.AUTH_PATH_LOGIN
        );
    }

    @Test
    void shouldReturnNotFound_whenLoginEmailDoesNotExist() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing-user@test.com",
                                  "password": "Password1!"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 404, MessageKey.AUTH_WRONG_EMAIL, AuthPaths.AUTH_PATH_LOGIN);
    }

    @Test
    void shouldReturnBadRequest_whenLoginPasswordIsWrong() throws Exception {
        int slot = FixtureNumbers.auth(6);
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(slot);
        userUtil.createUser(slot);

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "WrongPassword1!"
                                }
                                """.formatted(userCreateDTO.getEmail())))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 400, MessageKey.AUTH_WRONG_PASSWORD, AuthPaths.AUTH_PATH_LOGIN);
    }

    @Test
    void shouldReturnForbidden_whenLoginUserIsTemporarilyBanned() throws Exception {
        int slot = FixtureNumbers.auth(7);
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(slot);
        User user = userUtil.createUser(slot);
        user.setBannedUntil(Instant.now().plusSeconds(3_600));
        user.setBanReason(TestReportStrings.banReason("Temporary ban"));
        userRepository.save(user);

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(userCreateDTO.getEmail(), userCreateDTO.getPassword())))
                .andExpect(status().isForbidden())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 403, MessageKey.AUTH_TEMPORARILY_BANNED, AuthPaths.AUTH_PATH_LOGIN);
    }

    @Test
    void shouldReturnForbidden_whenLoginUserIsPermanentlyBanned() throws Exception {
        int slot = FixtureNumbers.auth(8);
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(slot);
        User user = userUtil.createUser(slot);
        user.setBannedPermanently(true);
        user.setBanReason(TestReportStrings.banReason("Permanent ban"));
        userRepository.save(user);

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(userCreateDTO.getEmail(), userCreateDTO.getPassword())))
                .andExpect(status().isForbidden())
                .andReturn();

        assertErrorResponse(responseBody(mvcResult), 403, MessageKey.AUTH_PERMANENTLY_BANNED, AuthPaths.AUTH_PATH_LOGIN);
    }

    @Test
    void shouldRefreshAccessToken_whenRefreshTokenIsValid() throws Exception {
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(FixtureNumbers.auth(9));
        userUtil.createUser(FixtureNumbers.auth(9));

        MvcResult loginResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(userCreateDTO.getEmail(), userCreateDTO.getPassword())))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = responseBody(loginResult).path("data").path("refreshToken").asText();
        AuthRefreshTokenDTO dto = AuthRefreshTokenDTO.builder().refreshToken(refreshToken).build();

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_REFRESH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("data").asText()).isNotBlank();
    }

    @Test
    void shouldReturnNotFound_whenRefreshTokenDoesNotExist() throws Exception {
        AuthRefreshTokenDTO dto = AuthRefreshTokenDTO.builder()
                .refreshToken("missing-refresh-token")
                .build();

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_REFRESH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.AUTH_WRONG_TOKEN,
                AuthPaths.AUTH_PATH_REFRESH_TOKEN
        );
    }

    @Test
    void shouldReturnBadRequest_whenRefreshTokenIsExpired() throws Exception {
        int slot = FixtureNumbers.auth(10);
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(slot);
        userUtil.createUser(slot);
        String refreshToken = loginAndGetRefreshToken(userCreateDTO);
        expireRefreshToken(refreshToken);

        AuthRefreshTokenDTO dto = AuthRefreshTokenDTO.builder()
                .refreshToken(refreshToken)
                .build();

        MvcResult mvcResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_REFRESH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.AUTH_REFRESH_TOKEN_EXPIRED,
                AuthPaths.AUTH_PATH_REFRESH_TOKEN
        );
        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();
    }

    @Test
    void shouldConfirmEmail_whenConfirmationTokenIsValid() throws Exception {
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(FixtureNumbers.auth(11));

        mockMvc.perform(post(AuthPaths.AUTH_PATH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateDTO)))
                .andExpect(status().isOk());

        User savedUser = userRepository.findByEmail(userCreateDTO.getEmail()).orElseThrow();
        String token = verificationTokenRepository
                .findByUserAndType(savedUser, TokenType.CONFIRM_EMAIL)
                .orElseThrow()
                .getToken();

        MvcResult mvcResult = mockMvc.perform(get(AuthPaths.AUTH_PATH_CONFIRM_REGISTRATION)
                        .queryParam("token", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        User verifiedUser = userRepository.findByEmail(userCreateDTO.getEmail()).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(verifiedUser.isEmailVerified()).isTrue();
        assertThat(verificationTokenRepository.findByToken(token)).isEmpty();
    }

    @Test
    void shouldReturnNotFound_whenConfirmationTokenDoesNotExist() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(AuthPaths.AUTH_PATH_CONFIRM_REGISTRATION)
                        .queryParam("token", "missing-confirm-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.AUTH_TOKEN_NOT_FOUND,
                AuthPaths.AUTH_PATH_CONFIRM_REGISTRATION
        );
    }

    @Test
    void shouldReturnBadRequest_whenConfirmationTokenIsExpired() throws Exception {
        int slot = FixtureNumbers.auth(12);
        String token = registerAndGetVerificationToken(slot, TokenType.CONFIRM_EMAIL);
        expireVerificationToken(token);

        MvcResult mvcResult = mockMvc.perform(get(AuthPaths.AUTH_PATH_CONFIRM_REGISTRATION)
                        .queryParam("token", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.AUTH_TOKEN_EXPIRED,
                AuthPaths.AUTH_PATH_CONFIRM_REGISTRATION
        );
        assertThat(verificationTokenRepository.findByToken(token)).isEmpty();
    }

    @Test
    void shouldReturnBadRequest_whenConfirmationTokenTypeIsWrong() throws Exception {
        int slot = FixtureNumbers.auth(13);
        User user = userUtil.createUser(slot);

        mockMvc.perform(patch(AuthPaths.AUTH_PATH_FORGOT_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UserForgotPasswordDTO.builder().email(user.getEmail()).build()
                        )))
                .andExpect(status().isOk());

        String token = verificationTokenRepository.findByUserAndType(user, TokenType.RESET_PASSWORD)
                .orElseThrow()
                .getToken();

        MvcResult mvcResult = mockMvc.perform(get(AuthPaths.AUTH_PATH_CONFIRM_REGISTRATION)
                        .queryParam("token", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.AUTH_TOKEN_NOT_VALID,
                AuthPaths.AUTH_PATH_CONFIRM_REGISTRATION
        );
    }

    @Test
    void shouldSendForgotPasswordEmail_whenForgotPasswordPayloadIsValid() throws Exception {
        User user = userUtil.createUser(FixtureNumbers.auth(14));
        UserForgotPasswordDTO dto = UserForgotPasswordDTO.builder()
                .email(user.getEmail())
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_FORGOT_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = responseBody(mvcResult);

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(verificationTokenRepository.findByUserAndType(user, TokenType.RESET_PASSWORD)).isPresent();
    }

    @Test
    void shouldReturnNotFound_whenForgotPasswordEmailDoesNotExist() throws Exception {
        UserForgotPasswordDTO dto = UserForgotPasswordDTO.builder()
                .email("missing-forgot-password@test.com")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_FORGOT_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.AUTH_EMAIL_NOT_FOUND,
                AuthPaths.AUTH_PATH_FORGOT_PASSWORD
        );
    }

    @Test
    void shouldReturnForbidden_whenForgotPasswordAccountIsNotVerified() throws Exception {
        int slot = FixtureNumbers.auth(15);
        User user = userUtil.createUser(slot, false);
        UserForgotPasswordDTO dto = UserForgotPasswordDTO.builder()
                .email(user.getEmail())
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_FORGOT_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                403,
                MessageKey.AUTH_ACCOUNT_NOT_VERIFIED,
                AuthPaths.AUTH_PATH_FORGOT_PASSWORD
        );
    }

    @Test
    void shouldResetForgottenPassword_whenResetTokenIsValid() throws Exception {
        User user = userUtil.createUser(FixtureNumbers.auth(16));

        mockMvc.perform(patch(AuthPaths.AUTH_PATH_FORGOT_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UserForgotPasswordDTO.builder().email(user.getEmail()).build()
                        )))
                .andExpect(status().isOk());

        String token = verificationTokenRepository
                .findByUserAndType(user, TokenType.RESET_PASSWORD)
                .orElseThrow()
                .getToken();

        UserResetForgottenPasswordDTO dto = UserResetForgottenPasswordDTO.builder()
                .newPassword("NewPassword1!")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_RESET_PASSWORD)
                        .queryParam("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        JsonNode body = responseBody(mvcResult);
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(passwordEncoder.matches("NewPassword1!", updatedUser.getPassword())).isTrue();
        assertThat(verificationTokenRepository.findByToken(token)).isEmpty();
    }

    @Test
    void shouldReturnNotFound_whenResetTokenDoesNotExist() throws Exception {
        UserResetForgottenPasswordDTO dto = UserResetForgottenPasswordDTO.builder()
                .newPassword("NewPassword1!")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_RESET_PASSWORD)
                        .queryParam("token", "missing-reset-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.AUTH_TOKEN_NOT_FOUND,
                AuthPaths.AUTH_PATH_RESET_PASSWORD
        );
    }

    @Test
    void shouldReturnBadRequest_whenResetTokenIsExpired() throws Exception {
        int slot = FixtureNumbers.auth(17);
        User user = userUtil.createUser(slot);

        mockMvc.perform(patch(AuthPaths.AUTH_PATH_FORGOT_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UserForgotPasswordDTO.builder().email(user.getEmail()).build()
                        )))
                .andExpect(status().isOk());

        String token = verificationTokenRepository.findByUserAndType(user, TokenType.RESET_PASSWORD)
                .orElseThrow()
                .getToken();
        expireVerificationToken(token);

        UserResetForgottenPasswordDTO dto = UserResetForgottenPasswordDTO.builder()
                .newPassword("NewPassword1!")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_RESET_PASSWORD)
                        .queryParam("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.AUTH_TOKEN_EXPIRED,
                AuthPaths.AUTH_PATH_RESET_PASSWORD
        );
        assertThat(verificationTokenRepository.findByToken(token)).isEmpty();
    }

    @Test
    void shouldReturnBadRequest_whenResetTokenTypeIsWrong() throws Exception {
        int slot = FixtureNumbers.auth(18);
        String token = registerAndGetVerificationToken(slot, TokenType.CONFIRM_EMAIL);
        UserResetForgottenPasswordDTO dto = UserResetForgottenPasswordDTO.builder()
                .newPassword("NewPassword1!")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_RESET_PASSWORD)
                        .queryParam("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.AUTH_TOKEN_NOT_VALID,
                AuthPaths.AUTH_PATH_RESET_PASSWORD
        );
    }

    @Test
    void shouldResendEmailConfirmation_whenAccountIsUnverified() throws Exception {
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(FixtureNumbers.auth(19));

        mockMvc.perform(post(AuthPaths.AUTH_PATH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateDTO)))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(userCreateDTO.getEmail()).orElseThrow();
        String oldToken = verificationTokenRepository
                .findByUserAndType(user, TokenType.CONFIRM_EMAIL)
                .orElseThrow()
                .getToken();

        UserResendEmailConfirmationDTO dto = UserResendEmailConfirmationDTO.builder()
                .email(user.getEmail())
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_RESEND_CONFIRMATION_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        String newToken = verificationTokenRepository
                .findByUserAndType(user, TokenType.CONFIRM_EMAIL)
                .orElseThrow()
                .getToken();

        assertThat(responseBody(mvcResult).path("success").asBoolean()).isTrue();
        assertThat(newToken).isNotBlank();
        assertThat(newToken).isNotEqualTo(oldToken);
    }

    @Test
    void shouldReturnBadRequest_whenResendEmailConfirmationAccountIsAlreadyVerified() throws Exception {
        int slot = FixtureNumbers.auth(20);
        User user = userUtil.createUser(slot);
        UserResendEmailConfirmationDTO dto = UserResendEmailConfirmationDTO.builder()
                .email(user.getEmail())
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_RESEND_CONFIRMATION_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.AUTH_ACCOUNT_ALREADY_VERIFIED,
                AuthPaths.AUTH_PATH_RESEND_CONFIRMATION_EMAIL
        );
    }

    @Test
    void shouldReturnNotFound_whenResendEmailConfirmationEmailDoesNotExist() throws Exception {
        UserResendEmailConfirmationDTO dto = UserResendEmailConfirmationDTO.builder()
                .email("missing-resend@test.com")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_RESEND_CONFIRMATION_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.AUTH_EMAIL_NOT_FOUND,
                AuthPaths.AUTH_PATH_RESEND_CONFIRMATION_EMAIL
        );
    }

    @Test
    void shouldInitiateDeleteAccount_whenEmailExists() throws Exception {
        User user = userUtil.createUser(FixtureNumbers.auth(21));
        UserInitiateDeleteAccountDTO dto = UserInitiateDeleteAccountDTO.builder()
                .email(user.getEmail())
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_INITIATE_DELETE_ACCOUNT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(responseBody(mvcResult).path("success").asBoolean()).isTrue();
        assertThat(verificationTokenRepository.findByUserAndType(user, TokenType.DELETE_ACCOUNT)).isPresent();
    }

    @Test
    void shouldReturnNotFound_whenInitiateDeleteAccountEmailDoesNotExist() throws Exception {
        UserInitiateDeleteAccountDTO dto = UserInitiateDeleteAccountDTO.builder()
                .email("missing-delete-account@test.com")
                .build();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_INITIATE_DELETE_ACCOUNT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.AUTH_EMAIL_NOT_FOUND,
                AuthPaths.AUTH_PATH_INITIATE_DELETE_ACCOUNT
        );
    }

    @Test
    void shouldDeleteAccount_whenDeleteTokenIsValid() throws Exception {
        User user = userUtil.createUser(FixtureNumbers.auth(22));

        mockMvc.perform(patch(AuthPaths.AUTH_PATH_INITIATE_DELETE_ACCOUNT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UserInitiateDeleteAccountDTO.builder().email(user.getEmail()).build()
                        )))
                .andExpect(status().isOk());

        String token = verificationTokenRepository
                .findByUserAndType(user, TokenType.DELETE_ACCOUNT)
                .orElseThrow()
                .getToken();

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_DELETE_ACCOUNT)
                        .queryParam("token", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        clearPersistenceContext();

        User deletedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(responseBody(mvcResult).path("success").asBoolean()).isTrue();
        assertThat(deletedUser.getDeletedAt()).isNotNull();
        assertThat(deletedUser.getEmail()).startsWith("anonymized-" + user.getId());
        assertThat(deletedUser.getNickname()).isEqualTo("anonymized-" + user.getId());
        assertThat(verificationTokenRepository.findByToken(token)).isEmpty();
    }

    @Test
    void shouldReturnNotFound_whenDeleteTokenDoesNotExist() throws Exception {
        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_DELETE_ACCOUNT)
                        .queryParam("token", "missing-delete-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                404,
                MessageKey.AUTH_TOKEN_NOT_FOUND,
                AuthPaths.AUTH_PATH_DELETE_ACCOUNT
        );
    }

    @Test
    void shouldReturnBadRequest_whenDeleteTokenIsExpired() throws Exception {
        int slot = FixtureNumbers.auth(23);
        User user = userUtil.createUser(slot);

        mockMvc.perform(patch(AuthPaths.AUTH_PATH_INITIATE_DELETE_ACCOUNT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UserInitiateDeleteAccountDTO.builder().email(user.getEmail()).build()
                        )))
                .andExpect(status().isOk());

        String token = verificationTokenRepository.findByUserAndType(user, TokenType.DELETE_ACCOUNT)
                .orElseThrow()
                .getToken();
        expireVerificationToken(token);

        MvcResult mvcResult = mockMvc.perform(patch(AuthPaths.AUTH_PATH_DELETE_ACCOUNT)
                        .queryParam("token", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorResponse(
                responseBody(mvcResult),
                400,
                MessageKey.AUTH_TOKEN_EXPIRED,
                AuthPaths.AUTH_PATH_DELETE_ACCOUNT
        );
        assertThat(verificationTokenRepository.findByToken(token)).isEmpty();
    }

    private String loginAndGetRefreshToken(UserCreateDTO userCreateDTO) throws Exception {
        MvcResult loginResult = mockMvc.perform(post(AuthPaths.AUTH_PATH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(userCreateDTO.getEmail(), userCreateDTO.getPassword())))
                .andExpect(status().isOk())
                .andReturn();

        return responseBody(loginResult).path("data").path("refreshToken").asText();
    }

    private String registerAndGetVerificationToken(int slot, TokenType tokenType) throws Exception {
        UserCreateDTO userCreateDTO = userUtil.buildUserCreateDTO(slot);

        mockMvc.perform(post(AuthPaths.AUTH_PATH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateDTO)))
                .andExpect(status().isOk());

        User savedUser = userRepository.findByEmail(userCreateDTO.getEmail()).orElseThrow();

        return verificationTokenRepository.findByUserAndType(savedUser, tokenType)
                .orElseThrow()
                .getToken();
    }

    private void expireRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token).orElseThrow();
        refreshToken.setExpiryDate(Instant.now().minusSeconds(60));
        refreshTokenRepository.save(refreshToken);

        clearPersistenceContext();
    }

    private void expireVerificationToken(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token).orElseThrow();
        verificationToken.setExpiryDate(Instant.now().minusSeconds(60));
        verificationTokenRepository.save(verificationToken);

        clearPersistenceContext();
    }
}
