package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.BanUserDTO;
import com.example.bookexchange.admin.dto.UserAdminDTO;
import com.example.bookexchange.admin.mapper.AdminMapper;
import com.example.bookexchange.auth.repository.RefreshTokenRepository;
import com.example.bookexchange.auth.repository.VerificationTokenRepository;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.util.ETagUtil;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import com.example.bookexchange.user.model.UserType;
import com.example.bookexchange.user.repository.UserRepository;
import com.example.bookexchange.user.specification.UserSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final AdminMapper adminMapper;
    private final AuditService auditService;
    private final VersionedEntityTransitionHelper versionedEntityTransitionHelper;

    @Transactional(readOnly = true)
    @Override
    public Result<Page<UserAdminDTO>> findUsers(
            Long userId,
            PageQueryDTO queryDTO,
            String searchText,
            Set<UserRole> roles,
            Boolean onlyBannedUsers,
            UserType userType
    ) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.ADMIN_USER_NOT_FOUND
                )
                .flatMap(user -> {
                    Boolean isUserSuperAdmin = user.getRoles().contains(UserRole.SUPER_ADMIN);

                    Specification<User> specification = UserSpecificationBuilder.build(searchText, roles, onlyBannedUsers, isUserSuperAdmin, userType);

                    Pageable pageable = PageRequest.of(
                            queryDTO.getPageIndex(),
                            queryDTO.getPageSize(),
                            Sort.by(Sort.Direction.DESC, "createdAt")
                    );

                    Page<UserAdminDTO> page = userRepository.findAll(specification, pageable).map(adminMapper::userToUserAdminDto);

                    return ResultFactory.ok(page);
                });
    }

    @Transactional(readOnly = true)
    @Override
    public Result<UserAdminDTO> findUserById(UserDetails adminUser, Long userId) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.ADMIN_USER_NOT_FOUND
                )
                .flatMap(user -> {
                            auditService.log(AuditEvent.builder()
                                    .action("ADMIN_USER_FIND")
                                    .result(AuditResult.SUCCESS)
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("userId", userId)
                                    .detail("userEmail", user.getEmail())
                                    .build()
                            );

                            return ResultFactory.okETag(
                                    adminMapper.userToUserAdminDto(user),
                                    ETagUtil.form(user)
                            );
                        }
                );
    }

    @Transactional
    @Override
    public Result<UserAdminDTO> giveAdminRights(UserDetails adminUser, Long userId) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.ADMIN_USER_NOT_FOUND
                )
                .flatMap(user -> {
                    if (!user.getRoles().contains(UserRole.ADMIN)) {
                        user.addRole(UserRole.ADMIN);

                        userRepository.save(user);
                    } else {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_GIVE_ADMIN_RIGHTS")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("USER_ALREADY_ADMIN")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("targetUserId", userId)
                                .detail("targetUserEmail", user.getEmail())
                                .build()
                        );

                        return ResultFactory.entityExists(MessageKey.ADMIN_USER_ALREADY_ADMIN, user.getEmail());
                    }

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_GIVE_ADMIN_RIGHTS")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("targetUserId", userId)
                            .detail("targetUserEmail", user.getEmail())
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.userToUserAdminDto(user),
                            MessageKey.ADMIN_RIGHTS_GIVEN,
                            ETagUtil.form(user),
                            user.getEmail()
                    );
                });
    }

    @Transactional
    @Override
    public Result<UserAdminDTO> revokeAdminRights(UserDetails adminUser, Long userId) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.ADMIN_USER_NOT_FOUND
                )
                .flatMap(user -> {
                    if (user.getRoles().contains(UserRole.ADMIN)) {
                        user.removeRole(UserRole.ADMIN);

                        userRepository.save(user);
                    } else {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_REVOKE_ADMIN_RIGHTS")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("USER_NOT_ADMIN")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("targetUserId", userId)
                                .detail("targetUserEmail", user.getEmail())
                                .build()
                        );

                        return ResultFactory.error(MessageKey.ADMIN_USER_NOT_ADMIN, HttpStatus.BAD_REQUEST, user.getEmail());
                    }

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_REVOKE_ADMIN_RIGHTS")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("targetUserId", userId)
                            .detail("targetUserEmail", user.getEmail())
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.userToUserAdminDto(user),
                            MessageKey.ADMIN_RIGHTS_REVOKED,
                            ETagUtil.form(user),
                            user.getEmail()
                    );
                });
    }

    @Transactional
    @Override
    public Result<UserAdminDTO> banUserById(UserDetails adminUser, Long userId, BanUserDTO banUserDTO, Long version) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.ADMIN_USER_NOT_FOUND
                )
                .flatMap(user -> {
                    Result<User> versionValidation = versionedEntityTransitionHelper.requireVersion(
                            user,
                            version,
                            "ADMIN_BAN_USER",
                            builder -> builder
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("targetUserId", userId)
                                    .detail("targetUserEmail", user.getEmail())
                                    .detail("BanUserDTO", banUserDTO)
                    );

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(adminMapper::userToUserAdminDto);
                    }

                    if (adminUser.getUsername().equals(user.getEmail())){
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_BAN_USER")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("ADMIN_CANT_BAN_YOURSELF")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("targetUserId", userId)
                                .detail("targetUserEmail", user.getEmail())
                                .detail("BanUserDTO", banUserDTO)
                                .build()
                        );

                        return ResultFactory.error(MessageKey.ADMIN_CANT_BAN_YOURSELF, HttpStatus.BAD_REQUEST);
                    }

                    if (banUserDTO.isBannedPermanently()) {
                        user.setBannedPermanently(true);
                    } else if (banUserDTO.getBannedUntil() != null) {
                        user.setBannedUntil(banUserDTO.getBannedUntil().toInstant());
                    } else {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_BAN_USER")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("ADMIN_REQUEST_NOT_VALID")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("targetUserId", userId)
                                .detail("targetUserEmail", user.getEmail())
                                .detail("BanUserDTO", banUserDTO)
                                .build()
                        );

                        return ResultFactory.error(MessageKey.ADMIN_REQUEST_NOT_VALID, HttpStatus.BAD_REQUEST);
                    }

                    user.setBanReason(banUserDTO.getBanReason());

                    refreshTokenRepository.deleteAll(new HashSet<>(user.getRefreshTokens()));

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_BAN_USER")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("targetUserId", userId)
                            .detail("targetUserEmail", user.getEmail())
                            .detail("BanUserDTO", banUserDTO)
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.userToUserAdminDto(user),
                            MessageKey.ADMIN_USER_BANNED,
                            ETagUtil.form(user),
                            user.getEmail()
                    );
                });
    }

    @Transactional
    @Override
    public Result<UserAdminDTO> unbanUserById(UserDetails adminUser, Long userId, Long version) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.ADMIN_USER_NOT_FOUND
                )
                .flatMap(user -> {
                    Result<User> versionValidation = versionedEntityTransitionHelper.requireVersion(
                            user,
                            version,
                            "ADMIN_UNBAN_USER",
                            builder -> builder
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("targetUserId", userId)
                                    .detail("targetUserEmail", user.getEmail())
                    );

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(adminMapper::userToUserAdminDto);
                    }

                    user.setBannedUntil(null);
                    user.setBannedPermanently(false);
                    user.setBanReason(null);

                    userRepository.save(user);

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_UNBAN_USER")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("targetUserId", userId)
                            .detail("targetUserEmail", user.getEmail())
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.userToUserAdminDto(user),
                            MessageKey.ADMIN_USER_UNBANNED,
                            ETagUtil.form(user),
                            user.getEmail()
                    );
                });
    }

    @Transactional
    @Override
    public Result<UserAdminDTO> deleteUser(UserDetails adminUser, Long userId, Long version) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.USER_ACCOUNT_NOT_FOUND
                )
                .flatMap(user -> {
                    Result<User> versionValidation = versionedEntityTransitionHelper.requireVersion(
                            user,
                            version,
                            "ADMIN_USER_DELETE",
                            builder -> builder
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("userId", userId)
                                    .detail("userEmail", user.getEmail())
                    );

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(adminMapper::userToUserAdminDto);
                    }

                    String oldUserEmail = user.getEmail();

                    user.setEmail("anonymized-" + user.getId() + "@anonymized.anonymized");
                    user.setNickname("anonymized-" + user.getId());
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
                            .action("ADMIN_USER_DELETE")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("userId", userId)
                            .detail("userEmail", user.getEmail())
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.userToUserAdminDto(user),
                            MessageKey.ADMIN_USER_DELETED,
                            ETagUtil.form(user),
                            oldUserEmail
                    );
                });
    }
}
