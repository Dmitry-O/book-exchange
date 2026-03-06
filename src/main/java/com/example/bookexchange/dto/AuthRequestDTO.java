package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("email")
    @NotBlank
    @NotNull
    @Email
    private String email;

    @JsonProperty("password")
    @NotBlank
    @NotNull
    private String password;
}
