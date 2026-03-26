package com.example.bookexchange.report.dto;

import com.example.bookexchange.report.model.TargetType;
import com.example.bookexchange.report.model.ReportReason;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ReportCreateDTO.ReportCreateDTOBuilder.class)
@Builder
@Data
public class ReportCreateDTO {

    @Schema(example = "USER")
    @JsonProperty("targetType")
    @NotNull
    private TargetType targetType;

    @Schema(example = "SPAM")
    @JsonProperty("reason")
    @NotNull
    private ReportReason reason;

    @Schema(example = "This user spams a lot by creating same books")
    @JsonProperty("comment")
    @NotBlank
    @Size(min = 3, max = 255)
    private String comment;
}
