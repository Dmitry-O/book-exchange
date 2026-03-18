package com.example.bookexchange.dto;

import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = AuthResponseDTO.AuthResponseDTOBuilder.class)
@Builder
@Data
public class AuthResponseDTO {

    private String accessToken;

    private String refreshToken;
}
