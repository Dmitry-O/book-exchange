package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.core.web.ResultResponseMapper;
import com.example.bookexchange.dto.ReportCreateDTO;
import com.example.bookexchange.services.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ResultResponseMapper responseMapper;

    public static final String REPORT_PATH_TARGET_ID = "/api/v1/report/{targetId}";

    @PostMapping(REPORT_PATH_TARGET_ID)
    public ResponseEntity<?> createReport(
            @CurrentUser Long userId,
            @PathVariable Long targetId,
            @Valid @RequestBody ReportCreateDTO reportCreateDTO,
            HttpServletRequest request
    ) {
        return responseMapper.map(
                reportService.createReport(
                        userId,
                        targetId,
                        reportCreateDTO
                ),
                request
        );
    }
}
