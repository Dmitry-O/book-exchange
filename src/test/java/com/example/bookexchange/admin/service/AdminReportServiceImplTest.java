package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.ReportAdminDTO;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.report.mapper.ReportMapper;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.repository.ReportRepository;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_REPORT_REJECTED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_REPORT_RESOLVED;
import static com.example.bookexchange.common.i18n.MessageKey.SYSTEM_OPTIMISTIC_LOCK;
import static com.example.bookexchange.common.result.ResultFactory.error;
import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private VersionedEntityTransitionHelper versionedEntityTransitionHelper;

    @InjectMocks
    private AdminReportServiceImpl adminReportService;

    @Test
    void shouldSetResolvedStatus_whenAdminResolvesReportWithCurrentVersion() {
        var reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        var report = UnitTestDataFactory.report(UnitFixtureIds.REPORT_ID, reporter, com.example.bookexchange.report.model.TargetType.USER, UnitFixtureIds.TARGET_USER_ID, ReportStatus.OPEN);
        ReportAdminDTO dto = org.mockito.Mockito.mock(ReportAdminDTO.class);
        var admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(versionedEntityTransitionHelper.requireVersion(any(), any(), any(), any())).thenReturn(ok(report));
        when(reportRepository.save(report)).thenReturn(report);
        when(reportMapper.reportToReportDto(report)).thenReturn(dto);

        Result<ReportAdminDTO> result = adminReportService.resolveReport(admin, report.getId(), report.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_REPORT_RESOLVED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    void shouldSetRejectedStatus_whenAdminRejectsReportWithCurrentVersion() {
        var reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        var report = UnitTestDataFactory.report(UnitFixtureIds.REPORT_ID, reporter, com.example.bookexchange.report.model.TargetType.USER, UnitFixtureIds.TARGET_USER_ID, ReportStatus.OPEN);
        ReportAdminDTO dto = org.mockito.Mockito.mock(ReportAdminDTO.class);
        var admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(versionedEntityTransitionHelper.requireVersion(any(), any(), any(), any())).thenReturn(ok(report));
        when(reportRepository.save(report)).thenReturn(report);
        when(reportMapper.reportToReportDto(report)).thenReturn(dto);

        Result<ReportAdminDTO> result = adminReportService.rejectReport(admin, report.getId(), report.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_REPORT_REJECTED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
    }

    @Test
    void shouldReturnConflict_whenAdminResolvesReportWithStaleVersion() {
        var reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        var report = UnitTestDataFactory.report(UnitFixtureIds.REPORT_ID, reporter, com.example.bookexchange.report.model.TargetType.USER, UnitFixtureIds.TARGET_USER_ID, ReportStatus.OPEN);
        var admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(versionedEntityTransitionHelper.requireVersion(any(), any(), any(), any()))
                .thenReturn(error(SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT));

        Result<ReportAdminDTO> result = adminReportService.resolveReport(admin, report.getId(), report.getVersion());

        assertFailure(result, SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
    }
}
