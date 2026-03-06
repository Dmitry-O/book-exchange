package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = AuthRefreshTokenDTO.AuthRefreshTokenDTOBuilder.class)
@Builder
@Data
public class AuthRefreshTokenDTO {

    @JsonProperty("refreshToken")
    @NotBlank
    @NotNull
    private String refreshToken;
}
