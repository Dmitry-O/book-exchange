package com.example.bookexchange.report.mapper;

import com.example.bookexchange.admin.dto.ReportAdminDTO;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.user.mapper.UserMapper;
import com.example.bookexchange.common.util.UrlBuilder;
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

    @AfterMapping
    protected void enrich(@MappingTarget ReportAdminDTO dto, Report report) {
        dto.setTargetUrl(urlBuilder.buildReportTargetUrl(
                report.getTargetType(),
                report.getTargetId()
        ));
        dto.setReporter(userMapper.userToUserDto(report.getReporter()));
    }
}
