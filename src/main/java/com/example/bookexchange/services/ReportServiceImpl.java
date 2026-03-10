package com.example.bookexchange.services;

import com.example.bookexchange.dto.ReportCreateDTO;
import com.example.bookexchange.exception.NotFoundException;
import com.example.bookexchange.models.Report;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.ReportRepository;
import com.example.bookexchange.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Override
    public String createReport(Long reporterId, Long targetId, ReportCreateDTO reportCreateDTO) {
        User reporter = userRepository.findById(reporterId).orElseThrow(() -> new NotFoundException("Der Benutzer mit ID " + reporterId + " wurde nicht gefunden"));

        Report report = Report
                .builder()
                .targetType(reportCreateDTO.getTargetType())
                .targetId(targetId)
                .reason(reportCreateDTO.getReason())
                .comment(reportCreateDTO.getComment())
                .reporter(reporter)
                .build();

        reportRepository.save(report);

        return "Ihre Beschwerde wurde gesendet";
    }
}
