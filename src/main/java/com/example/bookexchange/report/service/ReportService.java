package com.example.bookexchange.report.service;

import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.report.dto.ReportCreateDTO;
import com.example.bookexchange.report.dto.ReportDTO;
import org.springframework.data.domain.Page;

public interface ReportService {

    Result<Void> createReport(Long reporter, Long targetId, ReportCreateDTO reportCreateDTO);

    Result<Page<ReportDTO>> findUserReports(Long reporterId, PageQueryDTO queryDTO);
}
