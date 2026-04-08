package com.example.bookexchange.report.service;

import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.report.dto.ReportDTO;
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

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final AuditService auditService;
    private final ReportMapper reportMapper;

    @Transactional
    @Override
    public Result<Void> createReport(Long reporterId, Long targetId, ReportCreateDTO reportCreateDTO) {
        return validateTarget(reporterId, targetId, reportCreateDTO)
                .flatMap(reporter -> ensureNoDuplicateReport(reporter, targetId, reportCreateDTO))
                .flatMap(reporter -> saveReport(reporter, targetId, reportCreateDTO));
    }

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ReportDTO>> findUserReports(Long reporterId, PageQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPageIndex(),
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ReportDTO> reportPage = reportRepository.findByReporterId(reporterId, pageable)
                .map(reportMapper::reportToUserReportDto);

        return ResultFactory.ok(reportPage);
    }

    private Result<User> validateTarget(Long reporterId, Long targetId, ReportCreateDTO reportCreateDTO) {
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
                                .flatMap(u -> ResultFactory.ok(reporter));
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
                                .flatMap(b -> ResultFactory.ok(reporter));
                    }

                    return ResultFactory.error(MessageKey.SYSTEM_INVALID_DATA, HttpStatus.BAD_REQUEST);
                });
    }

    private Result<Void> saveReport(User reporter, Long targetId, ReportCreateDTO reportCreateDTO) {
        Report report = new Report(
                null,
                reportCreateDTO.getTargetType(),
                targetId,
                reportCreateDTO.getReason(),
                reportCreateDTO.getComment(),
                ReportStatus.OPEN,
                reporter
        );

        reportRepository.save(report);

        auditService.log(AuditEvent.builder()
                .action("REPORT_CREATE")
                .result(AuditResult.SUCCESS)
                .actorId(reporter.getId())
                .actorEmail(reporter.getEmail())
                .detail("targetId", targetId)
                .detail("reportCreateDTO", reportCreateDTO)
                .build()
        );

        return ResultFactory.okMessage(MessageKey.REPORT_SENT);
    }

    private Result<User> ensureNoDuplicateReport(User reporter, Long targetId, ReportCreateDTO reportCreateDTO) {
        boolean alreadyExists = reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporter.getId(),
                reportCreateDTO.getTargetType(),
                targetId
        );

        if (alreadyExists) {
            auditService.log(AuditEvent.builder()
                    .action("REPORT_CREATE")
                    .result(AuditResult.FAILURE)
                    .actorId(reporter.getId())
                    .actorEmail(reporter.getEmail())
                    .reason("REPORT_ALREADY_EXISTS")
                    .detail("targetId", targetId)
                    .detail("targetType", reportCreateDTO.getTargetType())
                    .build()
            );

            return ResultFactory.entityExists(MessageKey.REPORT_ALREADY_EXISTS);
        }

        return ResultFactory.ok(reporter);
    }
}
