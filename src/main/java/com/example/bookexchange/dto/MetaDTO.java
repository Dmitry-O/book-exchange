package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data
public class MetaDTO {

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("deletedAt")
    private Instant deletedAt;

    @JsonProperty("createdBy")
    private Long createdBy;

    @JsonProperty("updatedBy")
    private Long updatedBy;

    @JsonProperty("createdRequestId")
    private String createdRequestId;

    @JsonProperty("updatedRequestId")
    private String updatedRequestId;

}