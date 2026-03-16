package com.example.bookexchange.mappers;

import com.example.bookexchange.dto.ReportAdminDTO;
import com.example.bookexchange.dto.ReportCreateDTO;
import com.example.bookexchange.models.Report;
import com.example.bookexchange.util.UrlBuilder;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public abstract class ReportMapper {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UrlBuilder urlBuilder;

    public abstract ReportAdminDTO reportToReportDto(Report report);

    public abstract Report reportDtoToReport(ReportCreateDTO report);

    @AfterMapping
    protected void enrich(@MappingTarget ReportAdminDTO dto, Report report) {
        dto.setTargetUrl(urlBuilder.buildReportTargetUrl(
                report.getTargetType(),
                report.getTargetId()
        ));
        dto.setReporter(userMapper.userToUserDto(report.getReporter()));
    }
}
