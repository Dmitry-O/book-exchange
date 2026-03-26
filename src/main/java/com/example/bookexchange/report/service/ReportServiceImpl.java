package com.example.bookexchange.report.service;

import com.example.bookexchange.book.repository.BookRepository;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.report.dto.ReportCreateDTO;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.report.repository.ReportRepository;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final AuditService auditService;

    @Transactional
    @Override
    public Result<Void> createReport(Long reporterId, Long targetId, ReportCreateDTO reportCreateDTO) {
        return validateTarget(reporterId, targetId, reportCreateDTO)
                .flatMap(reporter -> saveReport(reporter, targetId, reportCreateDTO));
    }

    private Result<User> validateTarget(Long reporterId, Long targetId, ReportCreateDTO reportCreateDTO) {
        return ResultFactory.fromRepository(
                        userRepository,
                        reporterId,
                        MessageKey.USER_ACCOUNT_NOT_FOUND
                )
                .flatMap(reporter -> {
                    if (reportCreateDTO.getTargetType() == TargetType.USER) {
                        return ResultFactory.fromRepository(
                                        userRepository,
                                        targetId,
                                        MessageKey.USER_ACCOUNT_NOT_FOUND
                                )
                                .flatMap(u -> ResultFactory.ok(reporter));
                    } else {
                        return ResultFactory.fromRepository(
                                        bookRepository,
                                        targetId,
                                        MessageKey.BOOK_NOT_FOUND
                                )
                                .flatMap(b -> ResultFactory.ok(reporter));
                    }
                });
    }

    private Result<Void> saveReport(User reporter, Long targetId, ReportCreateDTO reportCreateDTO) {
        Report report = new Report(
                null,
                reportCreateDTO.getTargetType(),
                targetId,
                reportCreateDTO.getReason(),
                reportCreateDTO.getComment(),
                null,
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
}
