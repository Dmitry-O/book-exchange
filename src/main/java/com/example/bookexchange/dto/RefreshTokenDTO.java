package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefreshTokenDTO {

    @JsonProperty("token")
    @NotBlank
    @NotNull
    private String token;
}
