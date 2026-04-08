package com.example.bookexchange.report.repository;

import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, TargetType targetType, Long targetId);

    @EntityGraph(attributePaths = {"reporter"})
    Page<Report> findByStatusIn(Set<ReportStatus> reportStatuses, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter"})
    Page<Report> findByReporterId(Long reporterId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"reporter"})
    Page<Report> findAll(@NonNull Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"reporter"})
    Optional<Report> findById(Long id);
}
