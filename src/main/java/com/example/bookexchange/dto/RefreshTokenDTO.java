package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefreshTokenDTO {

    @Schema(example = "3ebf7aca-2a27-4b87-a6a8-e9420be17918")
    @JsonProperty("token")
    @NotBlank
    @NotNull
    private String token;
}
