package com.example.bookexchange.report.dto;

import com.example.bookexchange.report.model.ReportReason;
import com.example.bookexchange.report.model.ReportStatus;
import com.example.bookexchange.report.model.TargetType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportDTO {

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

    @JsonProperty("targetUser")
    private ReportTargetUserDTO targetUser;

    @JsonProperty("targetBook")
    private ReportTargetBookDTO targetBook;

    @Schema(example = "SPAM")
    @JsonProperty("reason")
    private ReportReason reason;

    @Schema(example = "This listing looks suspicious")
    @JsonProperty("comment")
    private String comment;

    @Schema(example = "OPEN")
    @JsonProperty("status")
    private ReportStatus status;

    @Schema(example = "2026-04-06T12:00:00Z")
    @JsonProperty("createdAt")
    private Instant createdAt;

    @Schema(example = "2026-04-06T12:15:00Z")
    @JsonProperty("updatedAt")
    private Instant updatedAt;
}
