package com.example.bookexchange.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record VerificationTokenValidationDTO(
        @Schema(example = "RESET_PASSWORD")
        VerificationTokenTypeDTO tokenType,

        @Schema(example = "2026-06-11T18:30:00Z")
        Instant expiresAt
) {
}
