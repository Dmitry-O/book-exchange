package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

@JsonDeserialize(builder = ApiErrorDTO.ApiErrorDTOBuilder.class)
@Builder
@Data
public class ApiErrorDTO {

    @NotNull
    @JsonProperty("status")
    private int status;

    @NotNull
    @NotBlank
    @JsonProperty("error")
    private String error;

    @NotNull
    @NotBlank
    @JsonProperty("message")
    private String message;

    @NotNull
    @NotBlank
    @JsonProperty("path")
    private String path;

    @NotNull
    @JsonProperty("timestamp")
    private Instant timestamp;

    @NotNull
    @NotBlank
    @JsonProperty("requestId")
    private String requestId;
}
