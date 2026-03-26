package com.example.bookexchange.admin.service;

import com.example.bookexchange.admin.dto.ReportAdminDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.report.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

public interface AdminReportService {

    Result<Page<ReportAdminDTO>> findReports(Integer pageIndex, Integer pageSize, Set<ReportStatus> statuses, String sortDirection);

    Result<ReportAdminDTO> findReportById(UserDetails adminUser, Long reportId);

    Result<ReportAdminDTO> resolveReport(UserDetails adminUser, Long reportId, Long version);

    Result<ReportAdminDTO> rejectReport(UserDetails adminUser, Long reportId, Long version);
}
