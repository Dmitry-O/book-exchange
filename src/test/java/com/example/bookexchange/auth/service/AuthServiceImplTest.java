package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.dto.AuthLoginRequestDTO;
import com.example.bookexchange.auth.dto.AuthLoginResponseDTO;
import com.example.bookexchange.auth.dto.VerificationTokenTypeDTO;
import com.example.bookexchange.auth.dto.VerificationTokenValidationDTO;
import com.example.bookexchange.auth.model.TokenType;
import com.example.bookexchange.auth.model.VerificationToken;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.email.EmailService;
import com.example.bookexchange.common.email.EmailType;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.notification.NotificationDispatchService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.security.auth.JwtService;
import com.example.bookexchange.support.TestReportStrings;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.dto.UserCreateDTO;
import com.example.bookexchange.user.dto.UserForgotPasswordDTO;
import com.example.bookexchange.user.dto.UserInitiateDeleteAccountDTO;
import com.example.bookexchange.user.dto.UserResendEmailConfirmationDTO;
import com.example.bookexchange.user.dto.UserResetForgottenPasswordDTO;
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
import static com.example.bookexchange.common.result.ResultFactory.error;
import static com.example.bookexchange.common.result.ResultFactory.successVoid;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private NotificationDispatchService notificationDispatchService;

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
    void shouldReturnUnauthorized_whenLoginPasswordIsWrong() {
        AuthLoginRequestDTO request = UnitTestDataFactory.loginRequest("reader@example.com", "wrong-password");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, request.getEmail(), "reader_one");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        Result<AuthLoginResponseDTO> result = authService.loginUser(request);

        assertFailure(result, MessageKey.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        verify(jwtService, never()).generateToken(any());
        verify(refreshTokenService, never()).createToken(any());
        verify(auditService).log(any());
    }

    @Test
    void shouldReturnSameUnauthorizedErrorAndPerformDummyPasswordCheck_whenLoginEmailDoesNotExist() {
        AuthLoginRequestDTO request = UnitTestDataFactory.loginRequest("missing@example.com", "wrong-password");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        Result<AuthLoginResponseDTO> result = authService.loginUser(request);

        assertFailure(result, MessageKey.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        verify(passwordEncoder).matches(eq(request.getPassword()), anyString());
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
    void shouldReturnTokenMetadataWithoutConsumingToken_whenVerificationTokenIsValid() {
        Instant expiresAt = Instant.now().plusSeconds(600);
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setExpiryDate(expiresAt);

        when(verificationTokenService.inspectToken(
                "reset-token",
                TokenType.RESET_PASSWORD,
                "VALIDATE_VERIFICATION_TOKEN"
        )).thenReturn(ok(verificationToken));

        Result<VerificationTokenValidationDTO> result = authService.validateVerificationToken(
                "reset-token",
                VerificationTokenTypeDTO.RESET_PASSWORD
        );

        VerificationTokenValidationDTO dto = assertSuccess(result, HttpStatus.OK).body();
        assertThat(dto.tokenType()).isEqualTo(VerificationTokenTypeDTO.RESET_PASSWORD);
        assertThat(dto.expiresAt()).isEqualTo(expiresAt);
        verify(verificationTokenService, never()).deleteToken(any());
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
    void shouldSendResetPasswordEmailAndReturnNeutralResponse_whenAccountIsEligible() {
        UserForgotPasswordDTO dto = UnitTestDataFactory.forgotPasswordDto("reader@example.com");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(verificationTokenService.ensureCooldownPassed(user, TokenType.RESET_PASSWORD)).thenReturn(successVoid());
        when(verificationTokenService.createToken(user, TokenType.RESET_PASSWORD)).thenReturn(ok("reset-token"));
        when(emailService.buildAndSendEmail(user, "reset-token", EmailType.RESET_PASSWORD)).thenReturn(successVoid());

        Result<Void> result = authService.forgotPassword(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(emailService).buildAndSendEmail(user, "reset-token", EmailType.RESET_PASSWORD);
        verify(auditService).log(any());
    }

    @Test
    void shouldReturnNeutralResponse_whenForgotPasswordAccountIsNotVerified() {
        UserForgotPasswordDTO dto = UnitTestDataFactory.forgotPasswordDto("reader@example.com");
        User user = UnitTestDataFactory.unverifiedUser(UnitFixtureIds.UNVERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));

        Result<Void> result = authService.forgotPassword(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(verificationTokenService, never()).createToken(any(), any());
    }

    @Test
    void shouldReturnNeutralResponse_whenForgotPasswordAccountDoesNotExist() {
        UserForgotPasswordDTO dto = UnitTestDataFactory.forgotPasswordDto("missing@example.com");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());

        Result<Void> result = authService.forgotPassword(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(verificationTokenService, never()).createToken(any(), any());
    }

    @Test
    void shouldReturnNeutralResponse_whenForgotPasswordIsRequestedTooSoon() {
        UserForgotPasswordDTO dto = UnitTestDataFactory.forgotPasswordDto("reader@example.com");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(verificationTokenService.ensureCooldownPassed(user, TokenType.RESET_PASSWORD))
                .thenReturn(error(MessageKey.SYSTEM_TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS));

        Result<Void> result = authService.forgotPassword(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(verificationTokenService, never()).createToken(any(), any());
    }

    @Test
    void shouldReturnNeutralResponse_whenResetPasswordEmailCannotBeSent() {
        UserForgotPasswordDTO dto = UnitTestDataFactory.forgotPasswordDto("reader@example.com");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(verificationTokenService.ensureCooldownPassed(user, TokenType.RESET_PASSWORD)).thenReturn(successVoid());
        when(verificationTokenService.createToken(user, TokenType.RESET_PASSWORD)).thenReturn(ok("reset-token"));
        when(emailService.buildAndSendEmail(user, "reset-token", EmailType.RESET_PASSWORD))
                .thenReturn(error(MessageKey.SYSTEM_UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));

        Result<Void> result = authService.forgotPassword(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(auditService, never()).log(any());
    }

    @Test
    void shouldReturnNeutralResponse_whenResendEmailConfirmationAccountIsAlreadyVerified() {
        UserResendEmailConfirmationDTO dto = UnitTestDataFactory.resendEmailConfirmationDto("reader@example.com");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));

        Result<Void> result = authService.resendEmailConfirmation(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(verificationTokenService, never()).createToken(any(), any());
    }

    @Test
    void shouldReturnNeutralResponse_whenResendEmailConfirmationIsRequestedTooSoon() {
        UserResendEmailConfirmationDTO dto = UnitTestDataFactory.resendEmailConfirmationDto("reader@example.com");
        User user = UnitTestDataFactory.unverifiedUser(UnitFixtureIds.UNVERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(verificationTokenService.ensureCooldownPassed(user, TokenType.CONFIRM_EMAIL))
                .thenReturn(error(MessageKey.SYSTEM_TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS));

        Result<Void> result = authService.resendEmailConfirmation(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(verificationTokenService, never()).deleteByUserAndType(any(), any());
    }

    @Test
    void shouldReturnNeutralResponse_whenResendEmailConfirmationAccountDoesNotExist() {
        UserResendEmailConfirmationDTO dto = UnitTestDataFactory.resendEmailConfirmationDto("missing@example.com");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());

        Result<Void> result = authService.resendEmailConfirmation(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(verificationTokenService, never()).createToken(any(), any());
    }

    @Test
    void shouldResendConfirmationEmailAndReturnNeutralResponse_whenAccountIsEligible() {
        UserResendEmailConfirmationDTO dto = UnitTestDataFactory.resendEmailConfirmationDto("reader@example.com");
        User user = UnitTestDataFactory.unverifiedUser(UnitFixtureIds.UNVERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(verificationTokenService.ensureCooldownPassed(user, TokenType.CONFIRM_EMAIL)).thenReturn(successVoid());
        when(verificationTokenService.createToken(user, TokenType.CONFIRM_EMAIL)).thenReturn(ok("confirm-token"));
        when(emailService.buildAndSendEmail(user, "confirm-token", EmailType.CONFIRM_EMAIL)).thenReturn(successVoid());

        Result<Void> result = authService.resendEmailConfirmation(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(verificationTokenService).deleteByUserAndType(user, TokenType.CONFIRM_EMAIL);
        verify(emailService).buildAndSendEmail(user, "confirm-token", EmailType.CONFIRM_EMAIL);
        verify(auditService).log(any());
    }

    @Test
    void shouldSendDeleteEmail_whenInitiateDeleteAccountUserExists() {
        UserInitiateDeleteAccountDTO dto = UnitTestDataFactory.initiateDeleteAccountDto("reader@example.com");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(verificationTokenService.ensureCooldownPassed(user, TokenType.DELETE_ACCOUNT)).thenReturn(successVoid());
        when(verificationTokenService.createToken(user, TokenType.DELETE_ACCOUNT)).thenReturn(ok("delete-token"));
        when(emailService.buildAndSendEmail(user, "delete-token", EmailType.DELETE_ACCOUNT))
                .thenReturn(okMessage(MessageKey.EMAIL_DELETE_ACCOUNT));

        Result<Void> result = authService.initiateDeleteAccount(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(auditService).log(any());
    }

    @Test
    void shouldReturnNeutralResponse_whenDeleteAccountEmailIsRequestedTooSoon() {
        UserInitiateDeleteAccountDTO dto = UnitTestDataFactory.initiateDeleteAccountDto("reader@example.com");
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, dto.getEmail(), "reader_one");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(verificationTokenService.ensureCooldownPassed(user, TokenType.DELETE_ACCOUNT))
                .thenReturn(error(MessageKey.SYSTEM_TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS));

        Result<Void> result = authService.initiateDeleteAccount(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(verificationTokenService, never()).createToken(any(), any());
    }

    @Test
    void shouldReturnNeutralResponse_whenDeleteAccountEmailDoesNotExist() {
        UserInitiateDeleteAccountDTO dto = UnitTestDataFactory.initiateDeleteAccountDto("missing@example.com");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());

        Result<Void> result = authService.initiateDeleteAccount(dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
        verify(verificationTokenService, never()).createToken(any(), any());
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

    @Test
    void shouldNotifyUser_whenForgottenPasswordIsReset() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(user);
        UserResetForgottenPasswordDTO dto = UserResetForgottenPasswordDTO.builder()
                .newPassword("NewPassword-123")
                .build();

        when(verificationTokenService.validateToken("reset-token", TokenType.RESET_PASSWORD, "RESET_FORGOTTEN_PASSWORD"))
                .thenReturn(ok(verificationToken));
        when(passwordEncoder.encode(dto.getNewPassword())).thenReturn("new-encoded-password");
        when(userRepository.save(user)).thenReturn(user);

        Result<Void> result = authService.resetForgottenPassword("reset-token", dto);

        assertSuccess(result, HttpStatus.OK, MessageKey.AUTH_PASSWORD_CHANGED);
        assertThat(user.getPassword()).isEqualTo("new-encoded-password");
        verify(verificationTokenService).deleteToken(verificationToken);
        verify(notificationDispatchService).sendPasswordChangedNotification(user);
    }

    @Test
    void shouldRejectSamePasswordAndKeepToken_whenForgottenPasswordIsReset() {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(user);
        UserResetForgottenPasswordDTO dto = UserResetForgottenPasswordDTO.builder()
                .newPassword("Password-123")
                .build();

        when(verificationTokenService.validateToken("reset-token", TokenType.RESET_PASSWORD, "RESET_FORGOTTEN_PASSWORD"))
                .thenReturn(ok(verificationToken));
        when(passwordEncoder.matches(dto.getNewPassword(), user.getPassword())).thenReturn(true);

        Result<Void> result = authService.resetForgottenPassword("reset-token", dto);

        assertFailure(result, MessageKey.AUTH_SAME_PASSWORDS, HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(any(User.class));
        verify(verificationTokenService, never()).deleteToken(any());
        verify(notificationDispatchService, never()).sendPasswordChangedNotification(any());
    }
}
