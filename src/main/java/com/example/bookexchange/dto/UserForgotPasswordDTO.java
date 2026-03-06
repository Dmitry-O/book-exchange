package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = UserForgotPasswordDTO.UserForgotPasswordDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserForgotPasswordDTO {

    @NotNull
    @NotBlank
    @Email
    @JsonProperty("email")
    private String email;
}
