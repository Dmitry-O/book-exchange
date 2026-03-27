package com.example.bookexchange.user.dto;

import com.example.bookexchange.common.validation.SupportedLocale;
import com.example.bookexchange.common.validation.StrongPassword;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UserCreateDTO {

    @Schema(example = "example@info.com")
    @JsonProperty("email")
    @NotBlank
    @Email
    private String email;

    @Schema(example = "password-12345")
    @JsonProperty("password")
    @NotBlank
    @Size(min = 8)
    @StrongPassword
    private String password;

    @Schema(example = "user12345")
    @JsonProperty("nickname")
    @NotBlank
    @Size(min = 5, max = 20)
    @Pattern(regexp = "^[\\p{L}\\p{N}_]+$", message = "{validation.nickname.pattern}")
    private String nickname;

    @Schema(example = "de")
    @JsonProperty("locale")
    @NotNull
    @SupportedLocale
    private String locale;
}
