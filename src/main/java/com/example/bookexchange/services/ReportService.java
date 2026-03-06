package com.example.bookexchange.services;

import com.example.bookexchange.dto.ReportCreateDTO;
import com.example.bookexchange.models.User;

public interface ReportService {

    Boolean createReport(User reporter, Long targetId, ReportCreateDTO reportCreateDTO);
}
