package com.example.bookexchange.common.demoaccess;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DemoAccessVerifyRequestDTO(
        @Schema(description = "Demo access token from the portfolio link", example = "demo-access-token")
        @JsonProperty("token")
        @NotBlank
        @Size(max = 512)
        String token
) { }
