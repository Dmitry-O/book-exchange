package com.example.bookexchange.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = AuthRefreshTokenDTO.AuthRefreshTokenDTOBuilder.class)
@Builder
@Data
public class AuthRefreshTokenDTO {

    @Schema(example = "3bc88331-60c0-467e-b4fa-e8958421ab68")
    @JsonProperty("refreshToken")
    @NotBlank
    @NotNull
    private String refreshToken;
}
