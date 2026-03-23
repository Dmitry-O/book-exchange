package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = UserResetForgottenPasswordDTO.UserResetForgottenPasswordDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResetForgottenPasswordDTO {

    @Schema(example = "new-password-12345")
    @NotNull
    @NotBlank
    @JsonProperty("newPassword")
    private String newPassword;
}
