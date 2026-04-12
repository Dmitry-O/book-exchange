package com.example.bookexchange.user.service;

import com.example.bookexchange.auth.dto.RefreshTokenDTO;
import com.example.bookexchange.auth.service.RefreshTokenService;
import com.example.bookexchange.auth.service.VerificationTokenService;
import com.example.bookexchange.book.service.BookService;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.storage.ImageStorageService;
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
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.Instant;

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
    private final VersionedEntityTransitionHelper versionedEntityTransitionHelper;
    private final ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    @Override
    public Result<UserDTO> getUser(Long userId) {
        return ResultFactory
                .fromRepository(userRepository, userId, MessageKey.USER_ACCOUNT_NOT_FOUND)
                .flatMap(user ->
                        ResultFactory.okETag(
                                userMapper.userToUserDto(user),
                                ETagUtil.form(user)
                        )
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
                    Result<User> versionValidation = validateUserVersion(user, version, "USER_UPDATE");

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(userMapper::userToUserDto);
                    }

                    if (dto.getNickname() != null &&
                            !user.getNickname().equals(dto.getNickname()) &&
                            userRepository.findByNickname(dto.getNickname()).isPresent()
                    ) {
                        return ResultFactory.error(MessageKey.AUTH_NICKNAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
                    }

                    userMapper.updateUserDtoToUser(dto, user);

                    Result<User> updatedUserResult = applyPhotoChange(user, dto.getPhotoBase64());

                    if (updatedUserResult.isFailure()) {
                        return rollbackOnFailure(updatedUserResult.map(userMapper::userToUserDto));
                    }

                    return updatedUserResult.flatMap(updatedUser -> {
                        logUserSuccess("USER_UPDATE", updatedUser.getId(), updatedUser.getEmail());

                        return ResultFactory.updated(
                                userMapper.userToUserDto(updatedUser),
                                MessageKey.USER_PROFILE_UPDATED,
                                ETagUtil.form(updatedUser)
                        );
                    });
                });
    }

    @Transactional
    @Override
    public Result<UserDTO> deleteUserPhoto(Long userId, Long version) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.USER_ACCOUNT_NOT_FOUND
                )
                .flatMap(user -> {
                    Result<User> versionValidation = validateUserVersion(user, version, "USER_PHOTO_DELETE");

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(userMapper::userToUserDto);
                    }

                    if (user.getPhotoUrl() == null || user.getPhotoUrl().isBlank()) {
                        return ResultFactory.updated(
                                userMapper.userToUserDto(user),
                                MessageKey.USER_PROFILE_PHOTO_DELETED,
                                ETagUtil.form(user)
                        );
                    }

                    return imageStorageService.deleteUserProfileImage(user.getId())
                            .flatMap(v -> {
                                user.setPhotoUrl(null);
                                User updatedUser = userRepository.save(user);

                                logUserSuccess("USER_PHOTO_DELETE", updatedUser.getId(), updatedUser.getEmail());

                                return ResultFactory.updated(
                                        userMapper.userToUserDto(updatedUser),
                                        MessageKey.USER_PROFILE_PHOTO_DELETED,
                                        ETagUtil.form(updatedUser)
                                );
                            });
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

                    Result<Void> deletedImagesResult = imageStorageService.deleteAllUserImages(user.getId());

                    if (deletedImagesResult.isFailure()) {
                        return deletedImagesResult.map(v -> null);
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
        return versionedEntityTransitionHelper.requireVersion(
                user,
                version,
                action,
                builder -> builder
                        .actorId(user.getId())
                        .actorEmail(user.getEmail())
        );
    }

    private void anonymizeUser(User user, Instant deletedAt) {
        user.setEmail("anonymized-" + user.getId() + "@anonymized.anonymized");
        user.setNickname("anonymized-" + user.getId());
        user.setPhotoUrl(null);
        user.setPassword("");
        user.setDeletedAt(deletedAt);
    }

    private Result<User> applyPhotoChange(User user, String photoBase64) {
        if (photoBase64 == null || photoBase64.isBlank()) {
            return ResultFactory.ok(userRepository.save(user));
        }

        return imageStorageService.replaceUserProfileImage(user.getId(), photoBase64)
                .flatMap(photoUrl -> {
                    user.setPhotoUrl(photoUrl);

                    return ResultFactory.ok(userRepository.save(user));
                });
    }

    private <T> Result<T> rollbackOnFailure(Result<T> result) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        return result;
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
