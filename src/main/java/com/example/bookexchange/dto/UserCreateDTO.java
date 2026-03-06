package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("email")
    @NotBlank
    @NotNull
    @Email
    private String email;

    @JsonProperty("password")
    @NotBlank
    @NotNull
    private String password;

    @JsonProperty("nickname")
    @NotBlank
    @NotNull
    @Size(min = 5, max = 20)
    private String nickname;
}
