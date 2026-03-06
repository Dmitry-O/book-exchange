package com.example.bookexchange.dto;

import com.example.bookexchange.models.ReportReason;
import com.example.bookexchange.models.TargetType;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("targetType")
    @NotNull
    private TargetType targetType;

    @JsonProperty("reason")
    @NotNull
    private ReportReason reason;

    @JsonProperty("comment")
    @NotBlank
    @Size(min = 3, max = 255)
    private String comment;
}
