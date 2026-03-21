package com.example.bookexchange.core.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

@JsonDeserialize(builder = ApiErrorDTO.ApiErrorDTOBuilder.class)
@Builder
@Data
public class ApiErrorDTO {

    @JsonProperty("status")
    private int status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonProperty("path")
    private String path;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("requestId")
    private String requestId;
}
