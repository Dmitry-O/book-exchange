package com.example.bookexchange.report.mapper;

import com.example.bookexchange.admin.dto.ReportAdminDTO;
import com.example.bookexchange.common.audit.dto.EntityAuditMetadataDTO;
import com.example.bookexchange.report.dto.ReportDTO;
import com.example.bookexchange.report.model.Report;
import com.example.bookexchange.user.mapper.UserMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public abstract class ReportMapper {

    protected UserMapper userMapper;

    @Autowired
    void setDependencies(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Mapping(target = "reporter", ignore = true)
    @Mapping(target = "meta", ignore = true)
    public abstract ReportAdminDTO reportToReportDto(Report report);

    public abstract ReportDTO reportToUserReportDto(Report report);

    @AfterMapping
    protected void enrich(@MappingTarget ReportAdminDTO dto, Report report) {
        dto.setReporter(userMapper.userToUserDto(report.getReporter()));
        dto.setMeta(toAuditMetadataDto(report));
    }

    private EntityAuditMetadataDTO toAuditMetadataDto(Report report) {
        EntityAuditMetadataDTO dto = new EntityAuditMetadataDTO();

        dto.setCreatedAt(report.getCreatedAt());
        dto.setUpdatedAt(report.getUpdatedAt());
        dto.setCreatedBy(report.getCreatedBy());
        dto.setUpdatedBy(report.getUpdatedBy());
        dto.setCreatedRequestId(report.getCreatedRequestId());
        dto.setUpdatedRequestId(report.getUpdatedRequestId());

        return dto;
    }
}
