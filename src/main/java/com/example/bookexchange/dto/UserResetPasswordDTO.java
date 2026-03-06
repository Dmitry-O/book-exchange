package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = UserResetPasswordDTO.UserResetPasswordDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResetPasswordDTO {

    @NotNull
    @NotBlank
    @JsonProperty("currentPassword")
    private String currentPassword;

    @NotNull
    @NotBlank
    @JsonProperty("newPassword")
    private String newPassword;
}
