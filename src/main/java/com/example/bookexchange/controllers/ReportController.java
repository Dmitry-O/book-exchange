package com.example.bookexchange.controllers;

import com.example.bookexchange.authentication.CurrentUser;
import com.example.bookexchange.dto.ApiMessage;
import com.example.bookexchange.dto.ReportCreateDTO;
import com.example.bookexchange.services.ReportService;
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

    public static final String REPORT_PATH_TARGET_ID = "/api/v1/report/{targetId}";

    @PostMapping(REPORT_PATH_TARGET_ID)
    public ResponseEntity<ApiMessage> createReport(@CurrentUser Long userId, @PathVariable Long targetId, @Valid @RequestBody ReportCreateDTO reportCreateDTO) {
        return ResponseEntity.ok(new ApiMessage(reportService.createReport(userId, targetId, reportCreateDTO)));
    }
}
