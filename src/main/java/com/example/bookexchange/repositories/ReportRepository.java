package com.example.bookexchange.repositories;

import com.example.bookexchange.models.Report;
import com.example.bookexchange.models.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatusIn(Set<ReportStatus> statuses, Pageable pageable);
}
