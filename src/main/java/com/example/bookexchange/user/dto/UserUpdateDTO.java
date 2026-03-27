package com.example.bookexchange.user.dto;

import com.example.bookexchange.common.validation.Base64Image;
import com.example.bookexchange.common.validation.SupportedLocale;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Pattern(regexp = "^[\\p{L}\\p{N}_]+$", message = "{validation.nickname.pattern}")
    private String nickname;

    @Schema(example = "User photo")
    @JsonProperty("photoBase64")
    @Base64Image
    private String photoBase64;

    @Schema(example = "de")
    @JsonProperty("locale")
    @SupportedLocale
    private String locale;
}
