package com.example.bookexchange.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = AuthResponseDTO.AuthResponseDTOBuilder.class)
@Builder
@Data
public class AuthResponseDTO {

    @NotBlank
    @NotBlank
    private String accessToken;

    @NotBlank
    @NotBlank
    private String refreshToken;
}
