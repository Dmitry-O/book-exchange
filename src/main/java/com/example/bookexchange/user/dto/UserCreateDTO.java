package com.example.bookexchange.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = UserCreateDTO.UserCreateDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateDTO {

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
    @Size(min = 8)
    private String password;

    @Schema(example = "user12345")
    @JsonProperty("nickname")
    @NotBlank
    @NotNull
    @Size(min = 5, max = 20)
    private String nickname;

    @Schema(example = "de")
    @JsonProperty("locale")
    @NotBlank
    private String locale;
}
