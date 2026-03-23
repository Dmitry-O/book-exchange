package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Data
public class MetaDTO {

    @Schema(example = "2026-03-23T13:00:58.815Z")
    @JsonProperty("createdAt")
    private Instant createdAt;

    @Schema(example = "2026-03-23T14:00:58.815Z")
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @Schema(example = "2026-03-23T14:00:58.815Z")
    @JsonProperty("deletedAt")
    private Instant deletedAt;

    @Schema(example = "1")
    @JsonProperty("createdBy")
    private Long createdBy;

    @Schema(example = "2")
    @JsonProperty("updatedBy")
    private Long updatedBy;

    @Schema(example = "4fa6b455-d7d0-4f90-9a07-df090a324401")
    @JsonProperty("createdRequestId")
    private String createdRequestId;

    @Schema(example = "3fa6b455-d7d0-4f90-9a07-df090a324401")
    @JsonProperty("updatedRequestId")
    private String updatedRequestId;

}