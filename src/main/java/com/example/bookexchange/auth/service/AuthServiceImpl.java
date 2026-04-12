package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.dto.AuthLoginRequestDTO;
import com.example.bookexchange.auth.dto.AuthLoginResponseDTO;
import com.example.bookexchange.auth.model.TokenType;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.email.EmailService;
import com.example.bookexchange.common.email.EmailType;
import com.example.bookexchange.common.i18n.MessageKey;
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

    private final UserService userService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final VerificationTokenService verificationTokenService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuditService auditService;

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
        return ResultFactory.fromOptional(
                        userRepository.findByEmail(requestDTO.getEmail()),
                        MessageKey.AUTH_WRONG_EMAIL
                )
                .flatMap(user -> {
                    if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
                        logAuthFailure("AUTH_LOGIN", user, "AUTH_WRONG_PASSWORD");

                        return ResultFactory.error(MessageKey.AUTH_WRONG_PASSWORD, HttpStatus.BAD_REQUEST);
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
                });
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
        return ResultFactory.fromOptional(
                        userRepository.findByEmail(dto.getEmail()),
                        MessageKey.AUTH_EMAIL_NOT_FOUND
                )
                .flatMap(user -> {
                    if (!user.isEmailVerified()) {
                        return ResultFactory.error(MessageKey.AUTH_ACCOUNT_NOT_VERIFIED, HttpStatus.FORBIDDEN);
                    }

                    return sendResetPasswordEmail(user)
                            .flatMap(v -> {
                                logAuthSuccess("FORGOT_PASSWORD", user);

                                return ResultFactory.successVoid();
                            });
                })
                .flatMap(v -> ResultFactory.okMessage(MessageKey.EMAIL_RESET_PASSWORD));
    }

    @Transactional
    @Override
    public Result<Void> resetForgottenPassword(String token, UserResetForgottenPasswordDTO dto) {
        return verificationTokenService.validateToken(token, TokenType.RESET_PASSWORD, "RESET_FORGOTTEN_PASSWORD")
                .flatMap(vt -> {
                    User user = vt.getUser();
                    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                    userRepository.save(user);

                    verificationTokenService.deleteToken(vt);

                    logAuthSuccess("RESET_FORGOTTEN_PASSWORD", user);

                    return ResultFactory.okMessage(MessageKey.AUTH_PASSWORD_CHANGED);
                });
    }

    @Transactional
    @Override
    public Result<Void> resendEmailConfirmation(UserResendEmailConfirmationDTO dto) {
        return ResultFactory.fromOptional(
                        userRepository.findByEmail(dto.getEmail()),
                        MessageKey.AUTH_EMAIL_NOT_FOUND
                )
                .flatMap(user -> {
                    if (user.isEmailVerified()) {
                        return ResultFactory.error(MessageKey.AUTH_ACCOUNT_ALREADY_VERIFIED, HttpStatus.BAD_REQUEST);
                    }

                    verificationTokenService.deleteByUserAndType(user, TokenType.CONFIRM_EMAIL);

                    return sendVerificationEmail(user)
                            .flatMap(v -> {
                                logAuthSuccess("RESEND_EMAIL_CONFIRMATION", user);

                                return ResultFactory.okMessage(MessageKey.EMAIL_VERIFY_ACCOUNT);
                            });
                });
    }

    @Transactional
    @Override
    public Result<Void> initiateDeleteAccount(UserInitiateDeleteAccountDTO dto) {
        return ResultFactory.fromOptional(
                        userRepository.findByEmail(dto.getEmail()),
                        MessageKey.AUTH_EMAIL_NOT_FOUND
                )
                .flatMap(user ->
                        sendDeleteAccountEmail(user)
                                .flatMap(v -> {
                                    logAuthSuccess("INITIATE_DELETE_ACCOUNT", user);

                                    return ResultFactory.okMessage(MessageKey.EMAIL_DELETE_ACCOUNT);
                                })
                );
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
