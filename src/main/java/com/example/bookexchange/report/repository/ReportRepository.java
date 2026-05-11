package com.example.bookexchange.report.repository;

import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
            Long reporterId,
            TargetType targetType,
            Long targetId,
            ReportStatus status
    );

    @EntityGraph(attributePaths = {"reporter"})
    Page<Report> findByStatusIn(Set<ReportStatus> reportStatuses, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter"})
    Page<Report> findByReporterId(Long reporterId, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter"})
    List<Report> findByTargetTypeAndTargetIdAndStatus(TargetType targetType, Long targetId, ReportStatus status);

    @Override
    @EntityGraph(attributePaths = {"reporter"})
    Page<Report> findAll(@NonNull Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"reporter"})
    Optional<Report> findById(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Report r
            SET r.targetUserNicknameSnapshot = null,
                r.targetBookNameSnapshot = null,
                r.targetBookOwnerUserIdSnapshot = null,
                r.targetBookOwnerNicknameSnapshot = null
            WHERE r.status IN :closedStatuses
                AND r.updatedAt < :updatedBefore
                AND (
                    r.targetUserNicknameSnapshot IS NOT NULL
                    OR r.targetBookNameSnapshot IS NOT NULL
                    OR r.targetBookOwnerUserIdSnapshot IS NOT NULL
                    OR r.targetBookOwnerNicknameSnapshot IS NOT NULL
                )
            """)
    int clearExpiredTargetSnapshots(
            @Param("updatedBefore") Instant updatedBefore,
            @Param("closedStatuses") Set<ReportStatus> closedStatuses
    );
}
