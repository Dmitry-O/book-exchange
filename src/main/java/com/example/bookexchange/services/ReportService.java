package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.dto.ReportCreateDTO;

public interface ReportService {

    Result<Void> createReport(Long reporter, Long targetId, ReportCreateDTO reportCreateDTO);
}
