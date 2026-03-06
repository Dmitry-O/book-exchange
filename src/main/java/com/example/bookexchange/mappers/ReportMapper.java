package com.example.bookexchange.mappers;

import com.example.bookexchange.dto.ReportDTO;
import com.example.bookexchange.models.Report;
import com.example.bookexchange.util.UrlBuilder;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ReportMapper {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UrlBuilder urlBuilder;

    public abstract ReportDTO reportToReportDto(Report report);

    @AfterMapping
    protected void enrich(@MappingTarget ReportDTO dto, Report report) {
        dto.setTargetUrl(urlBuilder.buildReportTargetUrl(
                report.getTargetType(),
                report.getTargetId()
        ));
        dto.setReporter(userMapper.userToUserDto(report.getReporter()));
    }
}
