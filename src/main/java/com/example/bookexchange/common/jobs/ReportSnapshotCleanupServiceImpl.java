package com.example.bookexchange.common.jobs;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportSnapshotCleanupServiceImpl implements ReportSnapshotCleanupService {

    private static final Set<ReportStatus> CLOSED_REPORT_STATUSES = Set.of(
            ReportStatus.RESOLVED,
            ReportStatus.REJECTED
    );

    private final ReportRepository reportRepository;
    private final AppProperties appProperties;

    @Transactional
    @Scheduled(cron = "${app.report.snapshot-cleanup-cron:0 45 3 * * *}")
    @Override
    public void deleteExpiredReportSnapshots() {
        int retentionDays = appProperties.getReport().getSnapshotRetentionDays();

        if (retentionDays <= 0) {
            log.info("Skipping report snapshot cleanup because retentionDays={}", retentionDays);
            return;
        }

        Instant updatedBefore = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int affectedReports = reportRepository.clearExpiredTargetSnapshots(updatedBefore, CLOSED_REPORT_STATUSES);

        log.info(
                "Cleared target snapshots for {} closed reports older than {}",
                affectedReports,
                updatedBefore
        );
    }
}
