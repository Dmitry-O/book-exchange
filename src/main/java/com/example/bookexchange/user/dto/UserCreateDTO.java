package com.example.bookexchange.user.dto;

import com.example.bookexchange.common.validation.DemoDataPolicyAccepted;
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

    @Schema(example = "example@info.com", format = "email", maxLength = 254)
    @JsonProperty("email")
    @NotBlank
    @Email(message = "{validation.email.format}")
    @Size(max = 254, message = "{validation.email.length}")
    private String email;

    @Schema(example = "Password-123!")
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

    @Schema(
            example = "true",
            description = "Required in the public demo environment to confirm that no real personal data should be entered"
    )
    @JsonProperty("demoDataPolicyAccepted")
    @DemoDataPolicyAccepted
    private Boolean demoDataPolicyAccepted;
}
