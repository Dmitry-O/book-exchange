package com.example.bookexchange.services;

import com.example.bookexchange.dto.ReportCreateDTO;
import com.example.bookexchange.models.Report;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.ReportRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

    @Override
    public Boolean createReport(User reporter, Long targetId, ReportCreateDTO reportCreateDTO) {
        try {
            Report report = Report
                    .builder()
                    .targetType(reportCreateDTO.getTargetType())
                    .targetId(targetId)
                    .reason(reportCreateDTO.getReason())
                    .comment(reportCreateDTO.getComment())
                    .reporter(reporter)
                    .build();

            reportRepository.save(report);

            return true;
        } catch(Exception e) {
            return false;
        }
    }
}
