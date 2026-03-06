package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = VerificationTokenDTO.VerificationTokenDTOBuilder.class)
@Builder
@Data
public class VerificationTokenDTO {

    @JsonProperty("message")
    @NotBlank
    @NotNull
    private String message;

    @JsonProperty("token")
    @NotBlank
    @NotNull
    private String token;
}
