package com.example.bookexchange.common.jobs;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportSnapshotCleanupServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    private ReportSnapshotCleanupServiceImpl reportSnapshotCleanupService;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getReport().setSnapshotRetentionDays(180);
        reportSnapshotCleanupService = new ReportSnapshotCleanupServiceImpl(reportRepository, appProperties);
    }

    @Test
    void shouldClearExpiredSnapshotsForClosedReports() {
        when(reportRepository.clearExpiredTargetSnapshots(any(), any())).thenReturn(3);

        reportSnapshotCleanupService.deleteExpiredReportSnapshots();

        verify(reportRepository).clearExpiredTargetSnapshots(
                any(),
                any(Set.class)
        );
    }

    @Test
    void shouldSkipCleanupWhenRetentionIsDisabled() {
        appProperties.getReport().setSnapshotRetentionDays(0);

        reportSnapshotCleanupService.deleteExpiredReportSnapshots();

        verify(reportRepository, never()).clearExpiredTargetSnapshots(any(), any());
    }
}
