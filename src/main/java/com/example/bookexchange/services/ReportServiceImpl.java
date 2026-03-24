package com.example.bookexchange.services;

import com.example.bookexchange.core.audit.AuditEvent;
import com.example.bookexchange.core.audit.AuditResult;
import com.example.bookexchange.core.audit.AuditService;
import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.core.result.ResultFactory;
import com.example.bookexchange.dto.ReportCreateDTO;
import com.example.bookexchange.models.MessageKey;
import com.example.bookexchange.models.Report;
import com.example.bookexchange.repositories.ReportRepository;
import com.example.bookexchange.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    @Override
    public Result<Void> createReport(Long reporterId, Long targetId, ReportCreateDTO reportCreateDTO) {
        return ResultFactory.fromRepository(
                        userRepository,
                        reporterId,
                        MessageKey.USER_ACCOUNT_NOT_FOUND
                )
                .flatMap(reporter -> {
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
                            .actorId(reporterId)
                            .actorEmail(reporter.getEmail())
                            .detail("targetId", targetId)
                            .detail("reportCreateDTO", reportCreateDTO)
                            .build()
                    );

                    return ResultFactory.okMessage(MessageKey.REPORT_SENT);
                });
    }
}
