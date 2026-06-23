package com.example.bookexchange.common.demoaccess;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record DemoAccessVerificationDTO(
        @Schema(description = "Whether demo access gate is active for this runtime", example = "true")
        @JsonProperty("enabled")
        boolean enabled,

        @Schema(description = "Expiration timestamp of the issued demo access cookie")
        @JsonProperty("expiresAt")
        Instant expiresAt
) { }
