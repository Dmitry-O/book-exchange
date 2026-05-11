package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.ReportAdminDTO;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.audit.service.VersionedEntityTransitionHelper;
import com.example.bookexchange.common.notification.NotificationDispatchService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.report.mapper.ReportMapper;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.report.repository.ReportRepository;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_REPORT_REJECTED;
import static com.example.bookexchange.common.i18n.MessageKey.ADMIN_REPORT_RESOLVED;
import static com.example.bookexchange.common.i18n.MessageKey.SYSTEM_OPTIMISTIC_LOCK;
import static com.example.bookexchange.common.result.ResultFactory.error;
import static com.example.bookexchange.common.result.ResultFactory.ok;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private AdminReportServiceImpl adminReportService;

    @Test
    void shouldSortOpenReportsFirstAndNewestFirst_whenAdminListsReports() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(reportRepository.findByStatusIn(eq(Set.of(ReportStatus.OPEN, ReportStatus.RESOLVED)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Result<org.springframework.data.domain.Page<ReportAdminDTO>> result = adminReportService.findReports(
                UnitTestDataFactory.pageQuery(0, 20),
                Set.of(ReportStatus.OPEN, ReportStatus.RESOLVED),
                com.example.bookexchange.common.dto.SortDirectionDTO.DESC
        );

        assertSuccess(result, HttpStatus.OK);
        verify(reportRepository).findByStatusIn(eq(Set.of(ReportStatus.OPEN, ReportStatus.RESOLVED)), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().toList()).extracting(order -> order.getProperty())
                .containsExactly("status", "createdAt", "id");
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("status").getDirection().name()).isEqualTo("ASC");
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection().name()).isEqualTo("DESC");
    }

    @Test
    void shouldSetResolvedStatus_whenAdminResolvesReportWithCurrentVersion() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        Report report = UnitTestDataFactory.report(UnitFixtureIds.REPORT_ID, reporter, TargetType.USER, UnitFixtureIds.TARGET_USER_ID, ReportStatus.OPEN);
        ReportAdminDTO dto = mock(ReportAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(versionedEntityTransitionHelper.requireVersion(any(), any(), any(), any())).thenReturn(ok(report));
        when(reportRepository.save(report)).thenReturn(report);
        when(reportMapper.reportToReportDto(report)).thenReturn(dto);

        Result<ReportAdminDTO> result = adminReportService.resolveReport(admin, report.getId(), report.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_REPORT_RESOLVED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(notificationDispatchService).sendReportResolvedNotification(report, admin.getUsername());
    }

    @Test
    void shouldSetRejectedStatus_whenAdminRejectsReportWithCurrentVersion() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        Report report = UnitTestDataFactory.report(UnitFixtureIds.REPORT_ID, reporter, TargetType.USER, UnitFixtureIds.TARGET_USER_ID, ReportStatus.OPEN);
        ReportAdminDTO dto = mock(ReportAdminDTO.class);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(versionedEntityTransitionHelper.requireVersion(any(), any(), any(), any())).thenReturn(ok(report));
        when(reportRepository.save(report)).thenReturn(report);
        when(reportMapper.reportToReportDto(report)).thenReturn(dto);

        Result<ReportAdminDTO> result = adminReportService.rejectReport(admin, report.getId(), report.getVersion());

        assertSuccess(result, HttpStatus.OK, ADMIN_REPORT_REJECTED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
        verify(notificationDispatchService).sendReportRejectedNotification(report, admin.getUsername());
    }

    @Test
    void shouldReturnConflict_whenAdminResolvesReportWithStaleVersion() {
        User reporter = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reporter@example.com", "reporter_one");
        Report report = UnitTestDataFactory.report(UnitFixtureIds.REPORT_ID, reporter, TargetType.USER, UnitFixtureIds.TARGET_USER_ID, ReportStatus.OPEN);
        UserDetails admin = UnitTestDataFactory.adminPrincipal("admin@example.com");

        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(versionedEntityTransitionHelper.requireVersion(any(), any(), any(), any()))
                .thenReturn(error(SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT));

        Result<ReportAdminDTO> result = adminReportService.resolveReport(admin, report.getId(), report.getVersion());

        assertFailure(result, SYSTEM_OPTIMISTIC_LOCK, HttpStatus.CONFLICT);
    }
}
