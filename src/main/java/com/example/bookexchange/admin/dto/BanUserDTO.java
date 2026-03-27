package com.example.bookexchange.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BanUserDTO {

    @Schema(example = "2026-04-21T12:00:00Z")
    @JsonProperty("bannedUntil")
    private OffsetDateTime bannedUntil;

    @Schema(example = "false")
    @JsonProperty("bannedPermanently")
    private boolean bannedPermanently = false;

    @Schema(example = "This user spams a lot with same books")
    @JsonProperty("banReason")
    @NotBlank
    @Size(min = 3, max = 255)
    private String banReason;

    @JsonIgnore
    @SuppressWarnings("unused")
    @AssertTrue(message = "{validation.ban.configuration}")
    public boolean isBanConfigurationValid() {
        return bannedPermanently || bannedUntil != null;
    }
}
