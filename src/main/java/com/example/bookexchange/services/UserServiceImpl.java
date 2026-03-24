package com.example.bookexchange.services;

import com.example.bookexchange.core.audit.AuditEvent;
import com.example.bookexchange.core.audit.AuditResult;
import com.example.bookexchange.core.audit.AuditService;
import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.core.result.ResultFactory;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.mappers.UserMapper;
import com.example.bookexchange.models.*;
import com.example.bookexchange.repositories.RefreshTokenRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.repositories.VerificationTokenRepository;
import com.example.bookexchange.util.ETagUtil;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.time.temporal.ChronoUnit.*;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private VerificationTokenRepository verificationTokenRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private EmailService emailService;
    private AuditService auditService;

    @Transactional(readOnly = true)
    @Override
    public Result<UserDTO> getUser(Long userId) {
        return ResultFactory
                .fromRepository(userRepository, userId, MessageKey.USER_ACCOUNT_NOT_FOUND)
                .map(user ->
                        ResultFactory.okETag(
                                userMapper.userToUserDto(user),
                                ETagUtil.form(user)
                        )
                )
                .flatMap(r -> r);
    }

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

        auditService.log(AuditEvent.builder()
                .action("CREATE_USER")
                .result(AuditResult.SUCCESS)
                .actorId(savedUser.getId())
                .actorEmail(savedUser.getEmail())
                .build()
        );

        return createVerificationToken(savedUser, TokenType.CONFIRM_EMAIL)
                .flatMap(token -> {
                            emailService.buildAndSendEmail(
                                    savedUser.getEmail(),
                                    token,
                                    EmailType.CONFIRM_EMAIL
                            );

                            return ResultFactory.okMessage(MessageKey.AUTH_ACCOUNT_REGISTERED);
                        }
                );
    }

    @Transactional
    @Override
    public Result<UserDTO> updateUser(Long userId, UserUpdateDTO dto, Long version) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.USER_ACCOUNT_NOT_FOUND
                )
                .flatMap(user -> {
                    if (!user.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("USER_UPDATE")
                                .result(AuditResult.FAILURE)
                                .actorId(user.getId())
                                .actorEmail(user.getEmail())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .build()
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    if (userRepository.findByNickname(dto.getNickname()).isPresent()) {
                        return ResultFactory.error(MessageKey.AUTH_NICKNAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
                    }

                    userMapper.updateUserDtoToUser(dto, user);
                    userRepository.save(user);

                    auditService.log(AuditEvent.builder()
                            .action("USER_UPDATE")
                            .result(AuditResult.SUCCESS)
                            .actorId(user.getId())
                            .actorEmail(user.getEmail())
                            .build()
                    );

                    return ResultFactory.updated(
                            userMapper.userToUserDto(user),
                            MessageKey.USER_PROFILE_UPDATED,
                            ETagUtil.form(user)
                    );
                });
    }

    @Transactional
    @Override
    public Result<Void> deleteUser(Long userId, Long version) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.USER_ACCOUNT_NOT_FOUND
                )
                .flatMap(user -> {
                    if (!user.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("USER_DELETE")
                                .result(AuditResult.FAILURE)
                                .actorId(user.getId())
                                .actorEmail(user.getEmail())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .build()
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    String oldUserEmail = user.getEmail();

                    user.setEmail("anonymized@anonymized.anonymized");
                    user.setNickname("anonymized");
                    user.setPhotoBase64(null);
                    user.setPassword("");
                    user.setDeletedAt(Instant.now());

                    for (Book book : new HashSet<>(user.getBooks())) {
                        if (book.getDeletedAt() == null) {
                            book.setDeletedAt(Instant.now());
                        }
                    }

                    refreshTokenRepository.deleteAll(new HashSet<>(user.getRefreshTokens()));
                    verificationTokenRepository.deleteAll(new HashSet<>(user.getVerificationToken()));

                    auditService.log(AuditEvent.builder()
                            .action("USER_DELETE")
                            .result(AuditResult.SUCCESS)
                            .actorId(userId)
                            .actorEmail(oldUserEmail)
                            .build()
                    );

                    return ResultFactory.okMessage(MessageKey.USER_ACCOUNT_DELETED);
                });
    }

    @Transactional
    @Override
    public Result<AuthResponseDTO> loginUser(AuthRequestDTO requestDTO) {
        return ResultFactory.fromOptional(
                        userRepository.findByEmail(requestDTO.getEmail()),
                        MessageKey.AUTH_WRONG_EMAIL
                )
                .flatMap(user -> {
                    if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
                        auditService.log(AuditEvent.builder()
                                .action("AUTH_LOGIN")
                                .result(AuditResult.FAILURE)
                                .actorId(user.getId())
                                .actorEmail(user.getEmail())
                                .reason("AUTH_WRONG_PASSWORD")
                                .build()
                        );

                        return ResultFactory.error(MessageKey.AUTH_WRONG_PASSWORD, HttpStatus.BAD_REQUEST);
                    }

                    if (!user.isEmailVerified()) {
                        verificationTokenRepository.findByUserAndType(user, TokenType.CONFIRM_EMAIL).ifPresent(verificationToken -> verificationTokenRepository.deleteById(verificationToken.getId()));

                        return ResultFactory.error(MessageKey.AUTH_ACCOUNT_NOT_VERIFIED, HttpStatus.FORBIDDEN);
                    }

                    Instant bannedUntil = user.getBannedUntil();
                    String banReason = user.getBanReason() != null ? user.getBanReason() : null;

                    if (user.isBannedPermanently()) {
                        auditService.log(AuditEvent.builder()
                                .action("AUTH_LOGIN")
                                .result(AuditResult.FAILURE)
                                .actorId(user.getId())
                                .actorEmail(user.getEmail())
                                .reason("AUTH_PERMANENTLY_BANNED")
                                .detail("banReason", banReason)
                                .build()
                        );

                        return ResultFactory.error(MessageKey.AUTH_PERMANENTLY_BANNED, HttpStatus.FORBIDDEN, banReason);
                    } else if (bannedUntil != null && bannedUntil.isAfter(Instant.now())) {
                        auditService.log(AuditEvent.builder()
                                .action("AUTH_LOGIN")
                                .result(AuditResult.FAILURE)
                                .actorId(user.getId())
                                .actorEmail(user.getEmail())
                                .reason("AUTH_TEMPORARILY_BANNED")
                                .detail("bannedUntil", bannedUntil)
                                .build()
                        );

                        return ResultFactory.error(MessageKey.AUTH_TEMPORARILY_BANNED, HttpStatus.FORBIDDEN, bannedUntil, banReason);
                    } else if (bannedUntil != null && bannedUntil.isBefore(Instant.now())) {
                        user.setBannedUntil(null);
                        user.setBanReason(null);

                        userRepository.save(user);
                    }

                    AuthResponseDTO authResponseDto = AuthResponseDTO
                            .builder()
                            .accessToken(jwtService.generateToken(user))
                            .refreshToken(createRefreshToken(user))
                            .build();

                    auditService.log(AuditEvent.builder()
                            .action("AUTH_LOGIN")
                            .result(AuditResult.SUCCESS)
                            .actorId(user.getId())
                            .actorEmail(user.getEmail())
                            .build()
                    );

                    return ResultFactory.ok(authResponseDto);
                });
    }

    private String createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plus(30, DAYS));

        return refreshTokenRepository.save(refreshToken).getToken();
    }

    private Result<String> createVerificationToken(User user, TokenType tokenType) {
        VerificationToken verificationToken = new VerificationToken();

        long tokenLiveTime;
        ChronoUnit timeUnit;

        switch (tokenType) {
            case CONFIRM_EMAIL -> {
                tokenLiveTime = 24L;
                timeUnit = HOURS;
            }
            case RESET_PASSWORD, DELETE_ACCOUNT -> {
                tokenLiveTime = 15L;
                timeUnit = MINUTES;
            }
            default -> {
                return ResultFactory.error(
                        MessageKey.AUTH_WRONG_TOKEN,
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        Instant expiryDate = Instant.now().plus(tokenLiveTime, timeUnit);

        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setExpiryDate(expiryDate);
        verificationToken.setType(tokenType);
        verificationToken.setUser(user);

        String token = verificationTokenRepository.save(verificationToken).getToken();

        return ResultFactory.ok(token);
    }

    @Transactional
    @Override
    public Result<String> refreshAccessToken(String token) {
        return ResultFactory.fromOptional(
                    refreshTokenRepository.findByToken(token),
                    MessageKey.AUTH_WRONG_TOKEN
                )
                .flatMap(refreshToken -> {
                    if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                        refreshTokenRepository.delete(refreshToken);

                        return ResultFactory.error(MessageKey.AUTH_REFRESH_TOKEN_EXPIRED, HttpStatus.BAD_REQUEST);
                    }

                    auditService.log(AuditEvent.builder()
                            .action("REFRESH_ACCESS_TOKEN")
                            .result(AuditResult.SUCCESS)
                            .actorId(refreshToken.getUser().getId())
                            .actorEmail(refreshToken.getUser().getEmail())
                            .build()
                    );

                    return ResultFactory.ok(jwtService.generateToken(refreshToken.getUser()));
                });
    }

    @Transactional
    @Override
    public Result<Void> resetPassword(Long userId, UserResetPasswordDTO dto, Long version) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.USER_ACCOUNT_NOT_FOUND
                )
                .flatMap(user -> {
                    if (!user.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("RESET_PASSWORD")
                                .result(AuditResult.FAILURE)
                                .actorId(userId)
                                .actorEmail(user.getEmail())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .build()
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    if (dto.getCurrentPassword().equals(dto.getNewPassword())) {
                        return ResultFactory.error(MessageKey.AUTH_SAME_PASSWORDS, HttpStatus.BAD_REQUEST);
                    } else if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                        auditService.log(AuditEvent.builder()
                                .action("RESET_PASSWORD")
                                .result(AuditResult.FAILURE)
                                .actorId(userId)
                                .actorEmail(user.getEmail())
                                .reason("AUTH_WRONG_ACTUAL_PASSWORD")
                                .build()
                        );

                        return ResultFactory.error(MessageKey.AUTH_WRONG_ACTUAL_PASSWORD, HttpStatus.BAD_REQUEST);
                    }

                    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                    userRepository.save(user);

                    auditService.log(AuditEvent.builder()
                            .action("RESET_PASSWORD")
                            .result(AuditResult.SUCCESS)
                            .actorId(userId)
                            .actorEmail(user.getEmail())
                            .build()
                    );

                    return ResultFactory.okMessage(MessageKey.AUTH_PASSWORD_CHANGED);
                });
    }

    @Transactional
    @Override
    public Result<Void> confirmRegistration(String token) {
        return ResultFactory.fromOptional(
                        verificationTokenRepository.findByToken(token),
                        MessageKey.AUTH_TOKEN_NOT_FOUND
                )
                .flatMap(vt -> {
                    if (vt.getExpiryDate().isBefore(Instant.now())) {
                        verificationTokenRepository.deleteById(vt.getId());

                        return ResultFactory.error(MessageKey.AUTH_TOKEN_EXPIRED, HttpStatus.BAD_REQUEST);
                    } else if (!vt.getType().equals(TokenType.CONFIRM_EMAIL)) {
                        auditService.log(AuditEvent.builder()
                                .action("CONFIRM_REGISTRATION")
                                .result(AuditResult.FAILURE)
                                .actorId(vt.getUser().getId())
                                .actorEmail(vt.getUser().getEmail())
                                .reason("AUTH_TOKEN_NOT_VALID")
                                .build()
                        );

                        return ResultFactory.error(MessageKey.AUTH_TOKEN_NOT_VALID, HttpStatus.BAD_REQUEST);
                    }

                    User user = vt.getUser();
                    user.setEmailVerified(true);
                    userRepository.save(user);

                    verificationTokenRepository.deleteById(vt.getId());

                    auditService.log(AuditEvent.builder()
                            .action("CONFIRM_REGISTRATION")
                            .result(AuditResult.SUCCESS)
                            .actorId(vt.getUser().getId())
                            .actorEmail(vt.getUser().getEmail())
                            .build()
                    );

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

                    return createVerificationToken(user, TokenType.RESET_PASSWORD)
                            .flatMap(token -> {
                                    emailService.buildAndSendEmail(
                                            user.getEmail(),
                                            token,
                                            EmailType.RESET_PASSWORD
                                    );

                                    auditService.log(AuditEvent.builder()
                                            .action("FORGOT_PASSWORD")
                                            .result(AuditResult.SUCCESS)
                                            .actorId(user.getId())
                                            .actorEmail(user.getEmail())
                                            .build()
                                    );

                                    return ResultFactory.successVoid();
                            });
                })
                .flatMap(v -> ResultFactory.okMessage(MessageKey.EMAIL_RESET_PASSWORD));
    }

    @Transactional
    @Override
    public Result<Void> resetForgottenPassword(String token, UserResetForgottenPasswordDTO dto) {
        return ResultFactory.fromOptional(
                        verificationTokenRepository.findByToken(token),
                        MessageKey.AUTH_TOKEN_NOT_FOUND
                )
                .flatMap(vt -> {
                    if (vt.getExpiryDate().isBefore(Instant.now())) {
                        verificationTokenRepository.deleteById(vt.getId());

                        return ResultFactory.error(MessageKey.AUTH_TOKEN_EXPIRED, HttpStatus.BAD_REQUEST);
                    } else if (!vt.getType().equals(TokenType.RESET_PASSWORD)) {
                        auditService.log(AuditEvent.builder()
                                .action("RESET_FORGOTTEN_PASSWORD")
                                .result(AuditResult.FAILURE)
                                .actorId(vt.getUser().getId())
                                .actorEmail(vt.getUser().getEmail())
                                .reason("RESET_PASSWORD_TOKEN_NOT_VALID")
                                .build()
                        );

                        return ResultFactory.error(MessageKey.AUTH_TOKEN_NOT_VALID, HttpStatus.BAD_REQUEST);
                    }

                    User user = vt.getUser();
                    user.setEmailVerified(true);
                    userRepository.save(user);

                    verificationTokenRepository.deleteById(vt.getId());

                    auditService.log(AuditEvent.builder()
                            .action("RESET_FORGOTTEN_PASSWORD")
                            .result(AuditResult.SUCCESS)
                            .actorId(vt.getUser().getId())
                            .actorEmail(vt.getUser().getEmail())
                            .build()
                    );

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

                    verificationTokenRepository.findByUserAndType(user, TokenType.CONFIRM_EMAIL).ifPresent(
                            verificationToken -> verificationTokenRepository.deleteById(verificationToken.getId())
                    );

                    return createVerificationToken(user, TokenType.CONFIRM_EMAIL)
                            .flatMap(token -> {
                                    emailService.buildAndSendEmail(
                                            user.getEmail(),
                                            token,
                                            EmailType.CONFIRM_EMAIL
                                    );

                                    auditService.log(AuditEvent.builder()
                                            .action("RESEND_EMAIL_CONFIRMATION")
                                            .result(AuditResult.SUCCESS)
                                            .actorId(user.getId())
                                            .actorEmail(user.getEmail())
                                            .build()
                                    );

                                    return ResultFactory.successVoid();
                            })
                            .flatMap(v -> ResultFactory.okMessage(MessageKey.EMAIL_VERIFY_ACCOUNT));
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
                        createVerificationToken(user, TokenType.DELETE_ACCOUNT)
                        .flatMap(token -> {
                                emailService.buildAndSendEmail(
                                        user.getEmail(),
                                        token,
                                        EmailType.DELETE_ACCOUNT
                                );

                                auditService.log(AuditEvent.builder()
                                        .action("INITIATE_DELETE_ACCOUNT")
                                        .result(AuditResult.SUCCESS)
                                        .actorId(user.getId())
                                        .actorEmail(user.getEmail())
                                        .build()
                                );

                                return ResultFactory.successVoid();
                        })
                        .flatMap(v -> ResultFactory.okMessage(MessageKey.EMAIL_DELETE_ACCOUNT))
                );
    }

    @Transactional
    @Override
    public Result<Void> deleteAccount(String token) {
        return ResultFactory.fromOptional(
                        verificationTokenRepository.findByToken(token),
                        MessageKey.AUTH_TOKEN_NOT_FOUND
                )
                .flatMap(vt -> deleteUser(vt.getUser().getId(), null));
    }

    @Transactional
    @Override
    public Result<Void> logout(Long userId, RefreshTokenDTO dto) {
        return ResultFactory.fromOptional(
                        refreshTokenRepository.findByTokenAndUserId(dto.getToken(), userId),
                        MessageKey.AUTH_TOKEN_NOT_FOUND
                )
                .flatMap(rt -> {
                    String userEmail = rt.getUser().getEmail();

                    refreshTokenRepository.delete(rt);

                    auditService.log(AuditEvent.builder()
                            .action("LOGOUT_USER")
                            .result(AuditResult.SUCCESS)
                            .actorId(userId)
                            .actorEmail(userEmail)
                            .build()
                    );

                    return ResultFactory.okMessage(MessageKey.AUTH_LOGOUT);
                });
    }
}
