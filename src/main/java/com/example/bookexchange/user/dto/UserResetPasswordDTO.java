package com.example.bookexchange.user.dto;

import com.example.bookexchange.common.validation.StrongPassword;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResetPasswordDTO {

    @Schema(example = "current-password-12345")
    @NotNull
    @NotBlank
    @JsonProperty("currentPassword")
    private String currentPassword;

    @Schema(example = "new-password-12345")
    @NotNull
    @NotBlank
    @StrongPassword
    @JsonProperty("newPassword")
    private String newPassword;
}
