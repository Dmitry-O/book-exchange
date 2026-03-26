package com.example.bookexchange.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = UserUpdateDTO.UserUpdateDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {

    @Schema(example = "example@info.com")
    @JsonProperty("email")
    @Email
    private String email;

    @Schema(example = "user_12345")
    @JsonProperty("nickname")
    @Size(min = 5, max = 20)
    private String nickname;

    @Schema(example = "User photo")
    @JsonProperty("photoBase64")
    private String photoBase64;

    @Schema(example = "de")
    @JsonProperty("locale")
    @NotBlank
    private String locale;
}
