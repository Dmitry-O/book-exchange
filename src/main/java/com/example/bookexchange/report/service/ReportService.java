package com.example.bookexchange.report.service;

import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.report.dto.ReportCreateDTO;

public interface ReportService {

    Result<Void> createReport(Long reporter, Long targetId, ReportCreateDTO reportCreateDTO);
}
