package com.example.bookexchange.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jdk.jfr.BooleanFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = BanUserDTO.BanUserDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BanUserDTO {

    @Schema(example = "2026-04-21 12:00:00")
    @JsonProperty("bannedUntil")
    @NotBlank
    @Size(min = 22, max = 32)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private String bannedUntil;

    @Schema(example = "false")
    @JsonProperty("bannedPermanently")
    @BooleanFlag
    private boolean bannedPermanently = false;

    @Schema(example = "This user spams a lot with same books")
    @JsonProperty("banReason")
    @NotBlank
    @NotNull
    @Size(min = 3, max = 255)
    private String banReason;
}
