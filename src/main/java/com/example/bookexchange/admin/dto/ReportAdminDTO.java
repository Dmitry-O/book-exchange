package com.example.bookexchange.admin.dto;

import com.example.bookexchange.common.audit.dto.EntityAuditMetadataDTO;
import com.example.bookexchange.report.model.ReportReason;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.user.dto.UserDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportAdminDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "3")
    @JsonProperty("version")
    private Long version;

    @Schema(example = "USER")
    @JsonProperty("targetType")
    private TargetType targetType;

    @Schema(example = "1")
    @JsonProperty("targetId")
    private Long targetId;

    @Schema(example = "SPAM")
    @JsonProperty("reason")
    private ReportReason reason;

    @Schema(example = "This user spams a lot with same books")
    @JsonProperty("comment")
    private String comment;

    @Schema(example = "OPEN")
    @JsonProperty("status")
    private ReportStatus status;

    @JsonProperty("reporter")
    private UserDTO reporter;

    @JsonProperty("meta")
    private EntityAuditMetadataDTO meta;
}
