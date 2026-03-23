package com.example.bookexchange.dto;

import com.example.bookexchange.models.ReportReason;
import com.example.bookexchange.models.ReportStatus;
import com.example.bookexchange.models.TargetType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ReportAdminDTO.ReportAdminDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportAdminDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "USER")
    @JsonProperty("targetType")
    private TargetType targetType;

    @Schema(example = "http://current_host/api/v1/admin/books/1")
    @JsonProperty("targetUrl")
    private String targetUrl;

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
    private MetaDTO meta;
}
