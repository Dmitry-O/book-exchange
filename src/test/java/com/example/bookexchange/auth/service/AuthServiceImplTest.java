package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.dto.AuthLoginRequestDTO;
import com.example.bookexchange.auth.dto.AuthLoginResponseDTO;
import com.example.bookexchange.auth.model.TokenType;
import com.example.bookexchange.auth.model.VerificationToken;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.email.EmailService;
import com.example.bookexchange.common.email.EmailType;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.security.auth.JwtService;
import com.example.bookexchange.support.TestReportStrings;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.dto.UserCreateDTO;
import com.example.bookexchange.user.dto.UserForgotPasswordDTO;
import com.example.bookexchange.user.dto.UserInitiateDeleteAccountDTO;
import com.example.bookexchange.user.dto.UserResendEmailConfirmationDTO;
import com.example.bookexchange.user.mapper.UserMapper;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.common.result.ResultFactory.okMessage;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldReturnForbidden_whenCreateUserMatchesSameUnverifiedAccount() {
        UserCreateDTO dto = UnitTestDataFactory.userCreateDto("reader@example.com", "reader_one");
        User existingUser = UnitTestDataFactory.unverifiedUser(
                UnitFixtureIds.UNVERIFIED_USER_ID,
                dto.getEmail(),
                dto.getNickname()
        );

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(dto.getPassword(), existingUser.getPassword())).thenReturn(true);

        Result<Void> result = authService.createUser(dto);

        assertFailure(result, MessageKey.EMAIL_VERIFY_ACCOUNT, HttpStatus.FORBIDDEN);
        verify(userMapper, never()).userCreateDtoToUser(any());
    }

    @Test
    void shouldReturnConflict_whenCreateUserNicknameAlreadyExists() {
        UserCreateDTO dto = UnitTestDataFactory.userCreateDto("reader@example.com", "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByNickname(dto.getNickname())).thenReturn(Optional.of(new User()));

        Result<Void> result = authService.createUser(dto);

        assertFailure(result, MessageKey.AUTH_NICKNAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }

    @Test
    void shouldPersistUserAndSendVerificationEmail_whenCreateUserPayloadIsValid() {
        UserCreateDTO dto = UnitTestDataFactory.userCreateDto("reader@example.com", "reader_one");
        User mappedUser = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, dto.getEmail(), dto.getNickname());
        mappedUser.setEmailVerified(false);

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByNickname(dto.getNickname())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encoded-password");
        when(userMapper.userCreateDtoToUser(dto)).thenReturn(mappedUser);
        when(userRepository.save(mappedUser)).thenReturn(mappedUser);
        when(verificationTokenService.createToken(mappedUser, TokenType.CONFIRM_EMAIL)).thenReturn(ok("verification-token"));
        when(emailService.buildAndSendEmail(mappedUser, "verification-token", EmailType.CONFIRM_EMAIL))
                .thenReturn(okMessage(MessageKey.EMAIL_VERIFY_ACCOUNT));

        Result<Void> result = authService.createUser(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.AUTH_ACCOUNT_REGISTERED);
        verify(userRepository).save(mappedUser);
        verify(auditService).log(any());
    }

    @Test
    void shouldReturnBadRequest_whenLoginPasswordIsWrong() {
        AuthLoginRequestDTO request = UnitTestDataFactory.loginRequest("reader@example.com", "wrong-password");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, request.getEmail(), "reader_one");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        Result<AuthLoginResponseDTO> result = authService.loginUser(request);

        assertFailure(result, MessageKey.AUTH_WRONG_PASSWORD, HttpStatus.BAD_REQUEST);
        verify(jwtService, never()).generateToken(any());
        verify(refreshTokenService, never()).createToken(any());
        verify(auditService).log(any());
    }

    @Test
    void shouldReturnForbidden_whenLoginAccountIsNotVerified() {
        AuthLoginRequestDTO request = UnitTestDataFactory.loginRequest("reader@example.com", "Password-123");
        User user = UnitTestDataFactory.unverifiedUser(UnitFixtureIds.UNVERIFIED_USER_ID, request.getEmail(), "reader_one");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);

        Result<AuthLoginResponseDTO> result = authService.loginUser(request);

        assertFailure(result, MessageKey.AUTH_ACCOUNT_NOT_VERIFIED, HttpStatus.FORBIDDEN);
        verify(verificationTokenService).deleteByUserAndType(user, TokenType.CONFIRM_EMAIL);
    }

    @Test
    void shouldReturnForbidden_whenLoginUserIsTemporarilyBanned() {
        AuthLoginRequestDTO request = UnitTestDataFactory.loginRequest("reader@example.com", "Password-123");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, request.getEmail(), "reader_one");
        user.setBannedUntil(Instant.now().plusSeconds(600));
        user.setBanReason(TestReportStrings.banReason("Spam"));

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);

        Result<AuthLoginResponseDTO> result = authService.loginUser(request);

        assertFailure(result, MessageKey.AUTH_TEMPORARILY_BANNED, HttpStatus.FORBIDDEN);
        verify(auditService).log(any());
    }

    @Test
    void shouldClearExpiredBanAndReturnTokens_whenLoginCredentialsAreValid() {
        AuthLoginRequestDTO request = UnitTestDataFactory.loginRequest("reader@example.com", "Password-123");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, request.getEmail(), "reader_one");
        user.setBannedUntil(Instant.now().minusSeconds(600));
        user.setBanReason(TestReportStrings.banReason("Old reason"));

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(refreshTokenService.createToken(user)).thenReturn("refresh-token");

        Result<AuthLoginResponseDTO> result = authService.loginUser(request);

        assertSuccess(result, HttpStatus.OK);
        assertThat(user.getBannedUntil()).isNull();
        assertThat(user.getBanReason()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void shouldMarkUserVerifiedAndDeleteToken_whenConfirmRegistrationTokenIsValid() {
        User user = UnitTestDataFactory.unverifiedUser(UnitFixtureIds.UNVERIFIED_USER_ID, "reader@example.com", "reader_one");
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(user);

        when(verificationTokenService.validateToken("confirm-token", TokenType.CONFIRM_EMAIL, "CONFIRM_REGISTRATION"))
                .thenReturn(ok(verificationToken));
        when(userRepository.save(user)).thenReturn(user);

        Result<Void> result = authService.confirmRegistration("confirm-token");

        assertSuccess(result, HttpStatus.OK, MessageKey.AUTH_REGISTRATION_COMPLETED);
        assertThat(user.isEmailVerified()).isTrue();
        verify(verificationTokenService).deleteToken(verificationToken);
    }

    @Test
    void shouldReturnForbidden_whenForgotPasswordAccountIsNotVerified() {
        UserForgotPasswordDTO dto = UnitTestDataFactory.forgotPasswordDto("reader@example.com");
        User user = UnitTestDataFactory.unverifiedUser(UnitFixtureIds.UNVERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));

        Result<Void> result = authService.forgotPassword(dto);

        assertFailure(result, MessageKey.AUTH_ACCOUNT_NOT_VERIFIED, HttpStatus.FORBIDDEN);
        verify(verificationTokenService, never()).createToken(any(), any());
    }

    @Test
    void shouldReturnBadRequest_whenResendEmailConfirmationAccountIsAlreadyVerified() {
        UserResendEmailConfirmationDTO dto = UnitTestDataFactory.resendEmailConfirmationDto("reader@example.com");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));

        Result<Void> result = authService.resendEmailConfirmation(dto);

        assertFailure(result, MessageKey.AUTH_ACCOUNT_ALREADY_VERIFIED, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldSendDeleteEmail_whenInitiateDeleteAccountUserExists() {
        UserInitiateDeleteAccountDTO dto = UnitTestDataFactory.initiateDeleteAccountDto("reader@example.com");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(verificationTokenService.createToken(user, TokenType.DELETE_ACCOUNT)).thenReturn(ok("delete-token"));
        when(emailService.buildAndSendEmail(user, "delete-token", EmailType.DELETE_ACCOUNT))
                .thenReturn(okMessage(MessageKey.EMAIL_DELETE_ACCOUNT));

        Result<Void> result = authService.initiateDeleteAccount(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_DELETE_ACCOUNT);
        verify(auditService).log(any());
    }

    @Test
    void shouldDelegateToUserService_whenDeleteAccountTokenIsValid() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        user.setVersion(7L);
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(user);

        when(verificationTokenService.validateToken("delete-token", TokenType.DELETE_ACCOUNT, "DELETE_ACCOUNT"))
                .thenReturn(ok(verificationToken));
        when(userService.deleteUser(user.getId(), user.getVersion())).thenReturn(okMessage(MessageKey.USER_ACCOUNT_DELETED));

        Result<Void> result = authService.deleteAccount("delete-token");

        assertSuccess(result, HttpStatus.OK, MessageKey.USER_ACCOUNT_DELETED);
        verify(userService).deleteUser(user.getId(), user.getVersion());
    }
}
