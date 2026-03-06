package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jdk.jfr.BooleanFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = BanUserDTO.BanUserDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BanUserDTO {

    @JsonProperty("bannedUntil")
    @NotBlank
    @Size(min = 22, max = 32)
    private String bannedUntil;

    @JsonProperty("bannedPermanently")
    @NotBlank
    @BooleanFlag
    private boolean bannedPermanently;

    @JsonProperty("banReason")
    @NotBlank
    @NotNull
    @Size(min = 3, max = 255)
    private String banReason;
}
