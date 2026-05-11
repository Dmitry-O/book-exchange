package com.example.bookexchange.report.service;

import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.service.SoftDeleteFilterHelper;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.notification.NotificationDispatchService;
import com.example.bookexchange.report.dto.ReportDTO;
import com.example.bookexchange.report.dto.ReportTargetBookDTO;
import com.example.bookexchange.report.dto.ReportTargetUserDTO;
import com.example.bookexchange.report.mapper.ReportMapper;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.report.dto.ReportCreateDTO;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.report.repository.ReportRepository;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final AuditService auditService;
    private final ReportMapper reportMapper;
    private final SoftDeleteFilterHelper softDeleteFilterHelper;
    private final NotificationDispatchService notificationDispatchService;

    @Transactional
    @Override
    public Result<Void> createReport(Long reporterId, Long targetId, ReportCreateDTO reportCreateDTO) {
        return validateTarget(reporterId, targetId, reportCreateDTO)
                .flatMap(target -> ensureNoDuplicateReport(target, targetId, reportCreateDTO))
                .flatMap(target -> saveReport(target, targetId, reportCreateDTO));
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ReportDTO>> findUserReports(Long reporterId, PageQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageIndex(),
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Report> reportPage = reportRepository.findByReporterId(reporterId, pageable);
        Map<Long, User> targetUsers = loadTargetUsers(reportPage.getContent());
        Map<Long, Book> targetBooks = loadTargetBooks(reportPage.getContent());
        Page<ReportDTO> responsePage = reportPage.map(report -> enrichUserReportDto(report, targetUsers, targetBooks));

        return ResultFactory.ok(responsePage);
    }

    private Result<ValidatedReportTarget> validateTarget(Long reporterId, Long targetId, ReportCreateDTO reportCreateDTO) {
        return ResultFactory.fromRepository(
                        userRepository,
                        reporterId,
                        MessageKey.USER_ACCOUNT_NOT_FOUND
                )
                .flatMap(reporter -> {
                    if (reportCreateDTO.getTargetType() == TargetType.USER) {
                        if (reporterId.equals(targetId)) {
                            return ResultFactory.error(MessageKey.REPORT_CANNOT_REPORT_YOURSELF, HttpStatus.BAD_REQUEST);
                        }

                        return ResultFactory.fromRepository(
                                        userRepository,
                                        targetId,
                                        MessageKey.USER_ACCOUNT_NOT_FOUND
                                )
                                .flatMap(targetUser -> ResultFactory.ok(new ValidatedReportTarget(reporter, targetUser, null)));
                    }

                    if (reportCreateDTO.getTargetType() == TargetType.BOOK) {
                        if (bookRepository.findByIdAndUserId(targetId, reporterId).isPresent()) {
                            return ResultFactory.error(MessageKey.REPORT_CANNOT_REPORT_YOUR_BOOK, HttpStatus.BAD_REQUEST);
                        }

                        return ResultFactory.fromRepository(
                                        bookRepository,
                                        targetId,
                                        MessageKey.BOOK_NOT_FOUND
                                )
                                .flatMap(targetBook -> ResultFactory.ok(new ValidatedReportTarget(reporter, null, targetBook)));
                    }

                    return ResultFactory.error(MessageKey.SYSTEM_INVALID_DATA, HttpStatus.BAD_REQUEST);
                });
    }

    private Result<Void> saveReport(ValidatedReportTarget target, Long targetId, ReportCreateDTO reportCreateDTO) {
        Report report = new Report(
                null,
                reportCreateDTO.getTargetType(),
                targetId,
                reportCreateDTO.getReason(),
                reportCreateDTO.getComment(),
                ReportStatus.OPEN,
                target.reporter()
        );

        if (reportCreateDTO.getTargetType() == TargetType.USER) {
            report.captureUserSnapshot(target.targetUser());
        }

        if (reportCreateDTO.getTargetType() == TargetType.BOOK) {
            report.captureBookSnapshot(target.targetBook());
        }

        reportRepository.save(report);
        notificationDispatchService.sendReportSubmittedNotifications(report);

        auditService.log(AuditEvent.builder()
                .action("REPORT_CREATE")
                .result(AuditResult.SUCCESS)
                .actorId(target.reporter().getId())
                .actorEmail(target.reporter().getEmail())
                .detail("targetId", targetId)
                .detail("reportCreateDTO", reportCreateDTO)
                .build()
        );

        return ResultFactory.okMessage(MessageKey.REPORT_SENT);
    }

    private Result<ValidatedReportTarget> ensureNoDuplicateReport(
            ValidatedReportTarget target,
            Long targetId,
            ReportCreateDTO reportCreateDTO
    ) {
        boolean alreadyExists = reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                target.reporter().getId(),
                reportCreateDTO.getTargetType(),
                targetId,
                ReportStatus.OPEN
        );

        if (alreadyExists) {
            auditService.log(AuditEvent.builder()
                    .action("REPORT_CREATE")
                    .result(AuditResult.FAILURE)
                    .actorId(target.reporter().getId())
                    .actorEmail(target.reporter().getEmail())
                    .reason("REPORT_ALREADY_EXISTS")
                    .detail("targetId", targetId)
                    .detail("targetType", reportCreateDTO.getTargetType())
                    .build()
            );

            return ResultFactory.entityExists(MessageKey.REPORT_ALREADY_EXISTS);
        }

        return ResultFactory.ok(target);
    }

    private ReportDTO enrichUserReportDto(
            Report report,
            Map<Long, User> targetUsers,
            Map<Long, Book> targetBooks
    ) {
        ReportDTO dto = reportMapper.reportToUserReportDto(report);

        if (report.getTargetType() == TargetType.USER) {
            User currentTargetUser = targetUsers.get(report.getTargetId());
            boolean targetDeleted = isDeleted(currentTargetUser);

            dto.setTargetDeleted(targetDeleted);
            dto.setTargetUser(buildUserTargetSnapshot(report, currentTargetUser, targetDeleted));
        }

        if (report.getTargetType() == TargetType.BOOK) {
            Book currentTargetBook = targetBooks.get(report.getTargetId());
            boolean targetDeleted = isDeleted(currentTargetBook);

            dto.setTargetDeleted(targetDeleted);
            dto.setTargetBook(buildBookTargetSnapshot(report, currentTargetBook, targetDeleted));
        }

        return dto;
    }

    private Map<Long, User> loadTargetUsers(List<Report> reports) {
        List<Long> targetUserIds = reports.stream()
                .filter(report -> report.getTargetType() == TargetType.USER)
                .map(Report::getTargetId)
                .distinct()
                .toList();

        if (targetUserIds.isEmpty()) {
            return Map.of();
        }

        return softDeleteFilterHelper.runWithoutDeletedFilter(() ->
                userRepository.findAllById(targetUserIds).stream()
                        .collect(Collectors.toMap(
                                User::getId,
                                Function.identity(),
                                (left, right) -> left
                        ))
        );
    }

    private Map<Long, Book> loadTargetBooks(List<Report> reports) {
        List<Long> targetBookIds = reports.stream()
                .filter(report -> report.getTargetType() == TargetType.BOOK)
                .map(Report::getTargetId)
                .distinct()
                .toList();

        if (targetBookIds.isEmpty()) {
            return Map.of();
        }

        return softDeleteFilterHelper.runWithoutDeletedFilter(() ->
                bookRepository.findAllByIdIn(targetBookIds).stream()
                        .collect(Collectors.toMap(
                                Book::getId,
                                Function.identity(),
                                (left, right) -> left
                        ))
        );
    }

    private ReportTargetUserDTO buildUserTargetSnapshot(Report report, User currentTargetUser, boolean targetDeleted) {
        ReportTargetUserDTO targetUser = reportMapper.reportToSnapshotTargetUserDto(report);

        if (targetUser == null) {
            targetUser = new ReportTargetUserDTO();
        }

        targetUser.setId(report.getTargetId());

        if (isBlank(targetUser.getNickname()) && !targetDeleted && currentTargetUser != null) {
            targetUser.setNickname(currentTargetUser.getNickname());
        }

        targetUser.setPhotoUrl(targetDeleted || currentTargetUser == null ? null : currentTargetUser.getPhotoUrl());

        return isBlank(targetUser.getNickname()) ? null : targetUser;
    }

    private ReportTargetBookDTO buildBookTargetSnapshot(Report report, Book currentTargetBook, boolean targetDeleted) {
        ReportTargetBookDTO targetBook = reportMapper.reportToSnapshotTargetBookDto(report);

        if (targetBook == null) {
            targetBook = new ReportTargetBookDTO();
        }

        targetBook.setId(report.getTargetId());

        if (!targetDeleted && currentTargetBook != null) {
            if (isBlank(targetBook.getName())) {
                targetBook.setName(currentTargetBook.getName());
            }

            if (targetBook.getOwnerUserId() == null && currentTargetBook.getUser() != null) {
                targetBook.setOwnerUserId(currentTargetBook.getUser().getId());
            }

            if (isBlank(targetBook.getOwnerNickname()) && currentTargetBook.getUser() != null) {
                targetBook.setOwnerNickname(currentTargetBook.getUser().getNickname());
            }

            targetBook.setPhotoUrl(currentTargetBook.getPhotoUrl());
            targetBook.setOwnerPhotoUrl(currentTargetBook.getUser() != null && !isDeleted(currentTargetBook.getUser())
                    ? currentTargetBook.getUser().getPhotoUrl()
                    : null);
        } else {
            targetBook.setPhotoUrl(null);
            targetBook.setOwnerPhotoUrl(null);
        }

        return isBlank(targetBook.getName()) ? null : targetBook;
    }

    private boolean isDeleted(User user) {
        return user == null || user.getDeletedAt() != null;
    }

    private boolean isDeleted(Book book) {
        return book == null || book.getDeletedAt() != null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ValidatedReportTarget(User reporter, User targetUser, Book targetBook) {
    }
}
