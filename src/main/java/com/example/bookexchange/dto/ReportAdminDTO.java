package com.example.bookexchange.dto;

import com.example.bookexchange.models.ReportReason;
import com.example.bookexchange.models.ReportStatus;
import com.example.bookexchange.models.TargetType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

@JsonDeserialize(builder = ReportAdminDTO.ReportAdminDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportAdminDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("targetType")
    private TargetType targetType;

    @JsonProperty("targetUrl")
    private String targetUrl;

    @JsonProperty("reason")
    private ReportReason reason;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("status")
    private ReportStatus status;

    @JsonProperty("reporter")
    private UserDTO reporter;

    @JsonProperty("meta")
    private MetaDTO meta;
}
