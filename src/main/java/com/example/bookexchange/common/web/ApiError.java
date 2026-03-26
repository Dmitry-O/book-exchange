package com.example.bookexchange.common.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.Instant;

@JsonDeserialize(builder = ApiError.ApiErrorBuilder.class)
@Builder
@Data
@Schema(name = "ApiError")
public class ApiError {

    @JsonProperty("status")
    private int status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @Schema(example = "/api/book/user/1")
    @JsonProperty("path")
    private String path;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @Schema(example = "3fa6b455-d7d0-4f90-9a07-df090a324401")
    @JsonProperty("requestId")
    private String requestId;
}
