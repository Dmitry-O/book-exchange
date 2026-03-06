package com.example.bookexchange.controllers;

import com.example.bookexchange.dto.ReportCreateDTO;
import com.example.bookexchange.models.User;
import com.example.bookexchange.services.ReportService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@AllArgsConstructor
public class ReportController {

    private final ReportService reportService;

    public static final String REPORT_PATH_TARGET_ID = "/api/v1/report/{targetId}";

    @PostMapping(REPORT_PATH_TARGET_ID)
    public ResponseEntity<String> createReport(@AuthenticationPrincipal User user, @PathVariable Long targetId, @Valid @RequestBody ReportCreateDTO reportCreateDTO) {
        if (!reportService.createReport(user, targetId, reportCreateDTO)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Berichtserstellung fehlgeschlagen");
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
