package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = AuthRequestDTO.AuthRequestDTOBuilder.class)
@Builder
@Data
public class AuthRequestDTO {

    @Schema(example = "example@info.com")
    @JsonProperty("email")
    @NotBlank
    @NotNull
    @Email
    private String email;

    @Schema(example = "password-12345")
    @JsonProperty("password")
    @NotBlank
    @NotNull
    private String password;
}
