package com.example.bookexchange.dto;

import com.example.bookexchange.models.ReportReason;
import com.example.bookexchange.models.ReportStatus;
import com.example.bookexchange.models.TargetType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

@JsonDeserialize(builder = ReportDTO.ReportDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportDTO {

    @JsonProperty("id")
    @NotNull
    private Long id;

    @JsonProperty("targetType")
    @NotNull
    private TargetType targetType;

    @JsonProperty("targetUrl")
    @NotNull
    @NotBlank
    private String targetUrl;

    @JsonProperty("reason")
    @NotNull
    private ReportReason reason;

    @JsonProperty("comment")
    @NotBlank
    private String comment;

    @JsonProperty("createdAt")
    @NotNull
    private Instant createdAt;

    @JsonProperty("status")
    @NotNull
    private ReportStatus status;

    @JsonProperty("reporter")
    @NotNull
    private UserDTO reporter;
}
