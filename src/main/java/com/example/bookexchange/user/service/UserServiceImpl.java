package com.example.bookexchange.user.service;

import com.example.bookexchange.auth.dto.RefreshTokenDTO;
import com.example.bookexchange.auth.repository.RefreshTokenRepository;
import com.example.bookexchange.auth.repository.VerificationTokenRepository;
import com.example.bookexchange.auth.service.RefreshTokenService;
import com.example.bookexchange.auth.service.VerificationTokenService;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.book.service.BookService;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.user.dto.*;
import com.example.bookexchange.common.util.ETagUtil;
import com.example.bookexchange.user.mapper.UserMapper;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokenService;
    private final VerificationTokenService verificationTokenService;
    private final BookService bookService;

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
    public Result<UserDTO> updateUser(Long userId, UserUpdateDTO dto, Long version) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.USER_ACCOUNT_NOT_FOUND
                )
                .flatMap(user -> {
                    Result<User> versionValidation = validateUserVersion(user, version, "USER_UPDATE");

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(userMapper::userToUserDto);
                    }

                    if (userRepository.findByNickname(dto.getNickname()).isPresent()) {
                        return ResultFactory.error(MessageKey.AUTH_NICKNAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
                    }

                    userMapper.updateUserDtoToUser(dto, user);
                    userRepository.save(user);

                    logUserSuccess("USER_UPDATE", user.getId(), user.getEmail());

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
                    Result<User> versionValidation = validateUserVersion(user, version, "USER_DELETE");

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(v -> null);
                    }

                    bookService.softDeleteBooks(user, Instant.now());

                    return ResultFactory.ok(user);
                })
                .flatMap(refreshTokenService::deleteUserTokens)
                .flatMap(verificationTokenService::deleteUserTokens)
                .flatMap(user -> {
                    String oldUserEmail = user.getEmail();

                    anonymizeUser(user, Instant.now());
                    logUserSuccess("USER_DELETE", userId, oldUserEmail);

                    return ResultFactory.okMessage(MessageKey.USER_ACCOUNT_DELETED);
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
                    Result<User> versionValidation = validateUserVersion(user, version, "RESET_PASSWORD");

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(v -> null);
                    }

                    if (dto.getCurrentPassword().equals(dto.getNewPassword())) {
                        return ResultFactory.error(MessageKey.AUTH_SAME_PASSWORDS, HttpStatus.BAD_REQUEST);
                    } else if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                        logUserFailure("RESET_PASSWORD", userId, user.getEmail(), "AUTH_WRONG_ACTUAL_PASSWORD");

                        return ResultFactory.error(MessageKey.AUTH_WRONG_ACTUAL_PASSWORD, HttpStatus.BAD_REQUEST);
                    }

                    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
                    userRepository.save(user);

                    logUserSuccess("RESET_PASSWORD", userId, user.getEmail());

                    return ResultFactory.okMessage(MessageKey.AUTH_PASSWORD_CHANGED);
                });
    }

    @Transactional
    @Override
    public Result<Void> logout(Long userId, RefreshTokenDTO dto) {
        return refreshTokenService.deleteToken(userId, dto.getToken())
                .flatMap(v -> ResultFactory.okMessage(MessageKey.AUTH_LOGOUT));
    }

    private Result<User> validateUserVersion(User user, Long version, String action) {
        if (!user.getVersion().equals(version)) {
            logUserFailure(action, user.getId(), user.getEmail(), "SYSTEM_OPTIMISTIC_LOCK");

            return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
        }

        return ResultFactory.ok(user);
    }

    private void anonymizeUser(User user, Instant deletedAt) {
        user.setEmail("anonymized@anonymized.anonymized");
        user.setNickname("anonymized");
        user.setPhotoBase64(null);
        user.setPassword("");
        user.setDeletedAt(deletedAt);
    }

    private void logUserSuccess(String action, Long actorId, String actorEmail) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.SUCCESS)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .build()
        );
    }

    private void logUserFailure(String action, Long actorId, String actorEmail, String reason) {
        auditService.log(AuditEvent.builder()
                .action(action)
                .result(AuditResult.FAILURE)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .reason(reason)
                .build()
        );
    }
}
