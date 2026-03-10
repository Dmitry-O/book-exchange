package com.example.bookexchange.services;

import com.example.bookexchange.dto.ReportCreateDTO;

public interface ReportService {

    String createReport(Long reporter, Long targetId, ReportCreateDTO reportCreateDTO);
}
