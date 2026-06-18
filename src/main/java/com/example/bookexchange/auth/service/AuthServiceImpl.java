package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.dto.AuthLoginRequestDTO;
import com.example.bookexchange.auth.dto.AuthLoginResponseDTO;
import com.example.bookexchange.auth.dto.VerificationTokenTypeDTO;
import com.example.bookexchange.auth.dto.VerificationTokenValidationDTO;
import com.example.bookexchange.auth.model.TokenType;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.email.EmailService;
import com.example.bookexchange.common.email.EmailType;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.notification.NotificationDispatchService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.security.auth.JwtService;
import com.example.bookexchange.user.dto.*;
import com.example.bookexchange.user.mapper.UserMapper;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserService userService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final VerificationTokenService verificationTokenService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final NotificationDispatchService notificationDispatchService;

    @Transactional
    @Override
    public Result<Void> createUser(UserCreateDTO dto) {
        String email = dto.getEmail();

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            if (passwordEncoder.matches(dto.getPassword(), userOptional.get().getPassword()) && !userOptional.get().isEmailVerified()) {
                return ResultFactory.error(MessageKey.EMAIL_VERIFY_ACCOUNT, HttpStatus.FORBIDDEN);
            }

            return ResultFactory.error(MessageKey.AUTH_EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT);
        }

        if (userRepository.findByNickname(dto.getNickname()).isPresent()) {
            return ResultFactory.entityExists(MessageKey.AUTH_NICKNAME_ALREADY_EXISTS);
        }

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));

        User user = userMapper.userCreateDtoToUser(dto);
        user.addRole(UserRole.USER);
        User savedUser = userRepository.save(user);

        logAuthSuccess("CREATE_USER", savedUser);

        return sendVerificationEmail(savedUser)
                .flatMap(v -> ResultFactory.okMessage(MessageKey.AUTH_ACCOUNT_REGISTERED));
    }

    @Transactional
    @Override
    public Result<AuthLoginResponseDTO> loginUser(AuthLoginRequestDTO requestDTO) {
        Optional<User> userOptional = userRepository.findByEmail(requestDTO.getEmail());

        if (userOptional.isEmpty()) {
            // Keep the missing-user path close to the password-mismatch path to reduce timing-based enumeration.
            passwordEncoder.matches(requestDTO.getPassword(), DUMMY_PASSWORD_HASH);
            logAuthFailure("AUTH_LOGIN", requestDTO.getEmail(), "AUTH_UNKNOWN_EMAIL");
            return ResultFactory.error(MessageKey.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            logAuthFailure("AUTH_LOGIN", user, "AUTH_WRONG_PASSWORD");

            return ResultFactory.error(MessageKey.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }

        if (!user.isEmailVerified()) {
            verificationTokenService.deleteByUserAndType(user, TokenType.CONFIRM_EMAIL);

            return ResultFactory.error(MessageKey.AUTH_ACCOUNT_NOT_VERIFIED, HttpStatus.FORBIDDEN);
        }

        Instant bannedUntil = user.getBannedUntil();
        String banReason = user.getBanReason() != null ? user.getBanReason() : null;

        if (user.isBannedPermanently()) {
            logAuthFailure("AUTH_LOGIN", user, "AUTH_PERMANENTLY_BANNED", "banReason", banReason);

            return ResultFactory.error(MessageKey.AUTH_PERMANENTLY_BANNED, HttpStatus.FORBIDDEN, banReason);
        } else if (bannedUntil != null && bannedUntil.isAfter(Instant.now())) {
            logAuthFailure("AUTH_LOGIN", user, "AUTH_TEMPORARILY_BANNED", "bannedUntil", bannedUntil);

            return ResultFactory.error(MessageKey.AUTH_TEMPORARILY_BANNED, HttpStatus.FORBIDDEN, bannedUntil, banReason);
        } else if (bannedUntil != null && bannedUntil.isBefore(Instant.now())) {
            user.setBannedUntil(null);
            user.setBanReason(null);

            userRepository.save(user);
        }

        AuthLoginResponseDTO authLoginResponseDto = AuthLoginResponseDTO
                .builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(refreshTokenService.createToken(user))
                .build();

        logAuthSuccess("AUTH_LOGIN", user);

        return ResultFactory.ok(authLoginResponseDto);
    }

    @Transactional
    @Override
    public Result<String> refreshAccessToken(String token) {
        return refreshTokenService.validateToken(token)
                .flatMap(refreshToken -> {
                    logAuthSuccess("REFRESH_ACCESS_TOKEN", refreshToken.getUser());

                    return ResultFactory.ok(jwtService.generateToken(refreshToken.getUser()));
                });
    }

    @Transactional(readOnly = true)
    @Override
    public Result<VerificationTokenValidationDTO> validateVerificationToken(
            String token,
            VerificationTokenTypeDTO tokenType
    ) {
        return verificationTokenService.inspectToken(
                        token,
                        tokenType.toTokenType(),
                        "VALIDATE_VERIFICATION_TOKEN"
                )
                .map(verificationToken -> new VerificationTokenValidationDTO(
                        tokenType,
                        verificationToken.getExpiryDate()
                ));
    }

    @Transactional
    @Override
    public Result<Void> confirmRegistration(String token) {
        return verificationTokenService.validateToken(token, TokenType.CONFIRM_EMAIL, "CONFIRM_REGISTRATION")
                .flatMap(vt -> {
                    User user = vt.getUser();
                    user.setEmailVerified(true);
                    userRepository.save(user);

                    verificationTokenService.deleteToken(vt);

                    logAuthSuccess("CONFIRM_REGISTRATION", user);

                    return ResultFactory.okMessage(MessageKey.AUTH_REGISTRATION_COMPLETED);
                });
    }

    @Transactional
    @Override
    public Result<Void> forgotPassword(UserForgotPasswordDTO dto) {
        Optional<User> userOptional = userRepository.findByEmail(dto.getEmail());

        if (userOptional.isEmpty() || !userOptional.get().isEmailVerified()) {
            return publicEmailActionResponse();
        }

        User user = userOptional.get();
        Result<Void> cooldownResult = verificationTokenService.ensureCooldownPassed(user, TokenType.RESET_PASSWORD);

        if (cooldownResult.isFailure()) {
            return publicEmailActionResponse();
        }

        return finishPublicEmailAction(sendResetPasswordEmail(user), "FORGOT_PASSWORD", user);
    }

    @Transactional
    @Override
    public Result<Void> resetForgottenPassword(String token, UserResetForgottenPasswordDTO dto) {
        return verificationTokenService.validateToken(token, TokenType.RESET_PASSWORD, "RESET_FORGOTTEN_PASSWORD")
                .flatMap(vt -> {
                    User user = vt.getUser();

                    if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
                        return ResultFactory.error(MessageKey.AUTH_SAME_PASSWORDS, HttpStatus.BAD_REQUEST);
                    }

                    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                    userRepository.save(user);

                    verificationTokenService.deleteToken(vt);

                    logAuthSuccess("RESET_FORGOTTEN_PASSWORD", user);
                    notificationDispatchService.sendPasswordChangedNotification(user);

                    return ResultFactory.okMessage(MessageKey.AUTH_PASSWORD_CHANGED);
                });
    }

    @Transactional
    @Override
    public Result<Void> resendEmailConfirmation(UserResendEmailConfirmationDTO dto) {
        Optional<User> userOptional = userRepository.findByEmail(dto.getEmail());

        if (userOptional.isEmpty() || userOptional.get().isEmailVerified()) {
            return publicEmailActionResponse();
        }

        User user = userOptional.get();
        Result<Void> cooldownResult = verificationTokenService.ensureCooldownPassed(user, TokenType.CONFIRM_EMAIL);

        if (cooldownResult.isFailure()) {
            return publicEmailActionResponse();
        }

        verificationTokenService.deleteByUserAndType(user, TokenType.CONFIRM_EMAIL);

        return finishPublicEmailAction(sendVerificationEmail(user), "RESEND_EMAIL_CONFIRMATION", user);
    }

    @Transactional
    @Override
    public Result<Void> initiateDeleteAccount(UserInitiateDeleteAccountDTO dto) {
        Optional<User> userOptional = userRepository.findByEmail(dto.getEmail());

        if (userOptional.isEmpty()) {
            return publicEmailActionResponse();
        }

        User user = userOptional.get();
        Result<Void> cooldownResult = verificationTokenService.ensureCooldownPassed(user, TokenType.DELETE_ACCOUNT);

        if (cooldownResult.isFailure()) {
            return publicEmailActionResponse();
        }

        return finishPublicEmailAction(sendDeleteAccountEmail(user), "INITIATE_DELETE_ACCOUNT", user);
    }

    @Transactional
    @Override
    public Result<Void> deleteAccount(String token) {
        return verificationTokenService.validateToken(token, TokenType.DELETE_ACCOUNT, "DELETE_ACCOUNT")
                .flatMap(vt -> userService.deleteUser(vt.getUser().getId(), vt.getUser().getVersion()));
    }

    private Result<Void> sendVerificationEmail(User user) {
        return sendEmailWithToken(user, TokenType.CONFIRM_EMAIL, EmailType.CONFIRM_EMAIL);
    }

    private Result<Void> sendResetPasswordEmail(User user) {
        return sendEmailWithToken(user, TokenType.RESET_PASSWORD, EmailType.RESET_PASSWORD);
    }

    private Result<Void> sendDeleteAccountEmail(User user) {
        return sendEmailWithToken(user, TokenType.DELETE_ACCOUNT, EmailType.DELETE_ACCOUNT);
    }

    private Result<Void> sendEmailWithToken(User user, TokenType tokenType, EmailType emailType) {
        return verificationTokenService.createToken(user, tokenType)
                .flatMap(token -> emailService.buildAndSendEmail(user, token, emailType));
    }

    private Result<Void> finishPublicEmailAction(Result<Void> emailResult, String action, User user) {
        if (emailResult.isSuccess()) {
            logAuthSuccess(action, user);
        }

        return publicEmailActionResponse();
    }

    private Result<Void> publicEmailActionResponse() {
        return ResultFactory.okMessage(MessageKey.EMAIL_PUBLIC_ACTION_REQUESTED);
    }

    private void logAuthSuccess(String action, User user) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.SUCCESS)
                .actorId(user.getId())
                .actorEmail(user.getEmail())
                .build()
        );
    }

    private void logAuthFailure(String action, User user, String reason) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.FAILURE)
                .actorId(user.getId())
                .actorEmail(user.getEmail())
                .reason(reason)
                .build()
        );
    }

    private void logAuthFailure(String action, String actorEmail, String reason) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.FAILURE)
                .actorEmail(actorEmail)
                .reason(reason)
                .build()
        );
    }

    private void logAuthFailure(String action, User user, String reason, String detailKey, Object detailValue) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.FAILURE)
                .actorId(user.getId())
                .actorEmail(user.getEmail())
                .reason(reason)
                .detail(detailKey, detailValue)
                .build()
        );
    }
}
