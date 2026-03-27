package com.example.bookexchange.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AuthLoginResponseDTO {

    @Schema(example = "eyJhbGciOiJIUzUx...")
    private String accessToken;

    @Schema(example = "3bc88331-60c0-467e-b4fa-e8958421ab68")
    private String refreshToken;
}
