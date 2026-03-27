package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.ReportAdminDTO;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.dto.SortDirectionDTO;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.util.ETagUtil;
import com.example.bookexchange.report.mapper.ReportMapper;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final AuditService auditService;
    private final VersionedEntityTransitionHelper versionedEntityTransitionHelper;

    @Transactional(readOnly = true)
    @Override
    public Result<Page<ReportAdminDTO>> findReports(PageQueryDTO queryDTO, Set<ReportStatus> reportStatuses, SortDirectionDTO sortDirection) {
        Pageable pageable;

        Sort.Direction direction = sortDirection == SortDirectionDTO.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        pageable = PageRequest.of(queryDTO.getPageIndex(), queryDTO.getPageSize(), Sort.by(direction, "createdAt"));

        Page<Report> reportPage;

        if (reportStatuses != null) {
            reportPage = reportRepository.findByStatusIn(reportStatuses, pageable);
        } else {
            reportPage = reportRepository.findAll(pageable);
        }

        return ResultFactory.ok(reportPage.map(reportMapper::reportToReportDto));
    }

    @Override
    public Result<ReportAdminDTO> findReportById(UserDetails adminUser, Long reportId) {
        return ResultFactory.fromRepository(
                        reportRepository,
                        reportId,
                        MessageKey.ADMIN_REPORT_NOT_FOUND
                )
                .flatMap(report -> {
                            auditService.log(AuditEvent.builder()
                                    .action("ADMIN_REPORT_FIND")
                                    .result(AuditResult.SUCCESS)
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("reportId", reportId)
                                    .build()
                            );

                            return ResultFactory.okETag(
                                    reportMapper.reportToReportDto(report),
                                    ETagUtil.form(report)
                            );
                        }
                );
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
                    Result<Report> versionValidation = versionedEntityTransitionHelper.requireVersion(
                            report,
                            version,
                            "ADMIN_REPORT_RESOLVE",
                            builder -> builder
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("reportId", reportId)
                    );

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(reportMapper::reportToReportDto);
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
                            reportMapper.reportToReportDto(report),
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
                    Result<Report> versionValidation = versionedEntityTransitionHelper.requireVersion(
                            report,
                            version,
                            "ADMIN_REPORT_REJECT",
                            builder -> builder
                                    .actorEmail(adminUser.getUsername())
                                    .detail("actorUserRoles", adminUser.getAuthorities())
                                    .detail("reportId", reportId)
                    );

                    if (versionValidation.isFailure()) {
                        return versionValidation.map(reportMapper::reportToReportDto);
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
                            reportMapper.reportToReportDto(report),
                            MessageKey.ADMIN_REPORT_REJECTED,
                            ETagUtil.form(report)
                    );
                });
    }
}
