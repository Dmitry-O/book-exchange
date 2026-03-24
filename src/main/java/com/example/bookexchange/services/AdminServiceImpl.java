package com.example.bookexchange.services;

import com.example.bookexchange.core.audit.AuditEvent;
import com.example.bookexchange.core.audit.AuditResult;
import com.example.bookexchange.core.audit.AuditService;
import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.core.result.ResultFactory;
import com.example.bookexchange.dto.*;
import com.example.bookexchange.mappers.*;
import com.example.bookexchange.models.*;
import com.example.bookexchange.repositories.*;
import com.example.bookexchange.specification.BookSpecificationBuilder;
import com.example.bookexchange.specification.UserSpecificationBuilder;
import com.example.bookexchange.util.ETagUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReportRepository reportRepository;
    private final ExchangeRepository exchangeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private VerificationTokenRepository verificationTokenRepository;
    private final BookMapper bookMapper;
    private final ReportMapper reportMapper;
    private final AdminMapper adminMapper;
    private final AuditService auditService;

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

    @Transactional(readOnly = true)
    @Override
    public Result<Page<UserAdminDTO>> findUsers(
            Long userId,
            Integer pageIndex,
            Integer pageSize,
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

                    Pageable pageable;

                    pageable = PageRequest.of(pageIndex, pageSize);

                    Page<UserAdminDTO> page = userRepository.findAll(specification, pageable).map(adminMapper::userToUserAdminDto);

                    return ResultFactory.ok(page);
                });
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<BookAdminDTO>> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize, BookType bookType) {
        Specification<Book> specification = BookSpecificationBuilder.build(dto, bookType);

        Pageable pageable;

        if (dto.getSortBy() != null && dto.getSortDirection() != null) {
            Sort.Direction direction = dto.getSortDirection().equalsIgnoreCase(("desc"))
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, dto.getSortBy()));
        } else {
            pageable = PageRequest.of(pageIndex, pageSize);
        }

        Page<BookAdminDTO> page = bookRepository.findAll(specification, pageable).map(adminMapper::bookToBookAdminDto);

        return ResultFactory.ok(page);
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
                    if (!user.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_BAN_USER")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("targetUserId", userId)
                                .detail("targetUserEmail", user.getEmail())
                                .detail("BanUserDTO", banUserDTO)
                                .build()
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
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
                        user.setBannedUntil(OffsetDateTime.parse(banUserDTO.getBannedUntil()).toInstant());
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
                    if (!user.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_UNBAN_USER")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("targetUserId", userId)
                                .detail("targetUserEmail", user.getEmail())
                                .build()
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
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
    public Result<BookAdminDTO> deleteBookById(UserDetails adminUser, Long bookId, Long version) {
        return ResultFactory.fromRepository(
                        bookRepository,
                        bookId,
                        MessageKey.ADMIN_BOOK_NOT_FOUND
                )
                .flatMap(book -> {
                    if (!book.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_BOOK_DELETE")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("bookId", bookId)
                                .detail("bookName", book.getName())
                                .build()
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    book.setDeletedAt(Instant.now());

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_BOOK_DELETE")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("bookId", bookId)
                            .detail("bookName", book.getName())
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.bookToBookAdminDto(book),
                            MessageKey.ADMIN_BOOK_DELETED,
                            ETagUtil.form(book),
                            book.getName()
                    );
                });
    }

    @Transactional
    @Override
    public Result<BookAdminDTO> updateBookById(UserDetails adminUser, Long bookId, BookUpdateDTO dto, Long version) {
        return ResultFactory.fromRepository(
                        bookRepository,
                        bookId,
                        MessageKey.ADMIN_BOOK_NOT_FOUND
                )
                .flatMap(book -> {
                    if (!book.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_BOOK_UPDATE")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("bookId", bookId)
                                .detail("bookName", book.getName())
                                .build()
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    bookMapper.updateBookDtoToBook(dto, book);
                    bookRepository.save(book);

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_BOOK_UPDATE")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("bookId", bookId)
                            .detail("bookName", book.getName())
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.bookToBookAdminDto(book),
                            MessageKey.ADMIN_BOOK_UPDATED,
                            ETagUtil.form(book),
                            book.getName()
                    );
                });
    }

    @Transactional(readOnly = true)
    @Override
    public Result<BookAdminDTO> findBookById(UserDetails adminUser, Long bookId) {
        return ResultFactory.fromRepository(
                        bookRepository,
                        bookId,
                        MessageKey.ADMIN_BOOK_NOT_FOUND
                )
                .map(book -> {
                            auditService.log(AuditEvent.builder()
                                    .action("ADMIN_BOOK_FIND")
                                    .result(AuditResult.SUCCESS)
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("bookId", bookId)
                                    .detail("bookName", book.getName())
                                    .build()
                            );

                            return ResultFactory.okETag(
                                    adminMapper.bookToBookAdminDto(book),
                                    ETagUtil.form(book)
                            );
                        }

                )
                .flatMap(r -> r);
    }

    @Transactional(readOnly = true)
    @Override
    public Result<UserAdminDTO> findUserById(UserDetails adminUser, Long userId) {
        return ResultFactory.fromRepository(
                        userRepository,
                        userId,
                        MessageKey.ADMIN_USER_NOT_FOUND
                )
                .map(user -> {
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
                )
                .flatMap(r -> r);
    }

    @Transactional
    @Override
    public Result<ReportAdminDTO> resolveReport(UserDetails adminUser, Long reportId, Long version) {
        return ResultFactory.fromRepository(
                        reportRepository,
                        reportId,
                        MessageKey.ADMIN_REPORT_NOT_FOUND
                )
                .flatMap(report -> {
                    if (!report.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_REPORT_RESOLVE")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("reportId", reportId)
                                .build()
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    report.setStatus(ReportStatus.RESOLVED);

                    reportRepository.save(report);

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_REPORT_RESOLVE")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("reportId", reportId)
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.reportToReportAdminDto(report),
                            MessageKey.ADMIN_REPORT_RESOLVED,
                            ETagUtil.form(report)
                    );
                });
    }

    @Transactional
    @Override
    public Result<ReportAdminDTO> rejectReport(UserDetails adminUser, Long reportId, Long version) {
        return ResultFactory.fromRepository(
                        reportRepository,
                        reportId,
                        MessageKey.ADMIN_REPORT_NOT_FOUND
                )
                .flatMap(report -> {
                    if (!report.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_REPORT_REJECT")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("reportId", reportId)
                                .build()
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    report.setStatus(ReportStatus.REJECTED);

                    reportRepository.save(report);

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_REPORT_REJECT")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("reportId", reportId)
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.reportToReportAdminDto(report),
                            MessageKey.ADMIN_REPORT_REJECTED,
                            ETagUtil.form(report)
                    );
                });
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ReportAdminDTO>> findReports(Integer pageIndex, Integer pageSize, Set<ReportStatus> reportStatuses, String sortDirection) {
        Pageable pageable;

        Sort.Direction direction = sortDirection.equalsIgnoreCase(("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, "createdAt"));

        Page<Report> reportPage;

        if (reportStatuses != null) {
            reportPage = reportRepository.findByStatusIn(reportStatuses, pageable);
        } else {
            reportPage = reportRepository.findAll(pageable);
        }

        return ResultFactory.ok(reportPage.map(reportMapper::reportToReportDto));
    }

    @Transactional
    @Override
    public Result<BookAdminDTO> restoreBookById(UserDetails adminUser, Long bookId, Long version) {
        return ResultFactory.fromRepository(
                        bookRepository,
                        bookId,
                        MessageKey.ADMIN_BOOK_NOT_FOUND
                )
                .flatMap(book -> {
                    if (!book.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_BOOK_RESTORE")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("bookId", bookId)
                                .detail("bookName", book.getName())
                                .build()
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
                    }

                    book.setDeletedAt(null);

                    auditService.log(AuditEvent.builder()
                            .action("ADMIN_BOOK_RESTORE")
                            .result(AuditResult.SUCCESS)
                            .actorEmail(adminUser.getUsername())
                            .detail("actorUserRoles", adminUser.getAuthorities())
                            .detail("bookId", bookId)
                            .detail("bookName", book.getName())
                            .build()
                    );

                    return ResultFactory.updated(
                            adminMapper.bookToBookAdminDto(book),
                            MessageKey.ADMIN_BOOK_RESTORED,
                            ETagUtil.form(book),
                            book.getName()
                    );
                });
    }

    @Override
    public Result<Page<ExchangeAdminDTO>> findExchanges(Integer pageIndex, Integer pageSize, Set<ExchangeStatus> exchangeStatuses) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Exchange> pendingExchangesPage;

        if (exchangeStatuses != null) {
            pendingExchangesPage = exchangeRepository.findByStatusIn(exchangeStatuses, pageable);
        } else {
            pendingExchangesPage = exchangeRepository.findAll(pageable);
        }

        return ResultFactory.ok(
                pendingExchangesPage.map(adminMapper::exchangeToExchangeAdminDto)
        );
    }

    @Override
    public Result<ExchangeAdminDTO> findExchangeById(UserDetails adminUser, Long exchangeId) {
        return ResultFactory.fromRepository(
                exchangeRepository,
                exchangeId,
                MessageKey.ADMIN_EXCHANGE_NOT_FOUND
        )
                .map(exchange -> {
                            auditService.log(AuditEvent.builder()
                                    .action("ADMIN_EXCHANGE_FIND")
                                    .result(AuditResult.SUCCESS)
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("exchangeId", exchangeId)
                                    .build()
                            );

                            return ResultFactory.okETag(
                                    adminMapper.exchangeToExchangeAdminDto(exchange),
                                    ETagUtil.form(exchange)
                            );
                        }
                )
                .flatMap(r -> r);
    }

    @Override
    public Result<ReportAdminDTO> findReportById(UserDetails adminUser, Long reportId) {
        return ResultFactory.fromRepository(
                        reportRepository,
                        reportId,
                        MessageKey.ADMIN_REPORT_NOT_FOUND
                )
                .map(report -> {
                            auditService.log(AuditEvent.builder()
                                    .action("ADMIN_REPORT_FIND")
                                    .result(AuditResult.SUCCESS)
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("reportId", reportId)
                                    .build()
                            );

                            return ResultFactory.okETag(
                                    adminMapper.reportToReportAdminDto(report),
                                    ETagUtil.form(report)
                            );
                        }
                )
                .flatMap(r -> r);
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
                    if (!user.getVersion().equals(version)) {
                        auditService.log(AuditEvent.builder()
                                .action("ADMIN_USER_DELETE")
                                .result(AuditResult.FAILURE)
                                .actorEmail(adminUser.getUsername())
                                .reason("SYSTEM_OPTIMISTIC_LOCK")
                                .detail("actorUserRoles", adminUser.getAuthorities())
                                .detail("userId", userId)
                                .detail("userEmail", user.getEmail())
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
