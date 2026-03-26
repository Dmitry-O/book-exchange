package com.example.bookexchange.user.dto;

import com.example.bookexchange.user.model.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Set;

@JsonDeserialize(builder = UserDTO.UserDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "example@info.com")
    @JsonProperty("email")
    private String email;

    @Schema(example = "user_12345")
    @JsonProperty("nickname")
    private String nickname;

    @Schema(example = "User photo")
    @JsonProperty("photoBase64")
    private String photoBase64;

    @Schema(example = "2026-04-21 12:00:00")
    @JsonProperty("bannedUntil")
    private String bannedUntil;

    @Schema(example = "false")
    @JsonProperty("bannedPermanently")
    private boolean bannedPermanently;

    @Schema(example = "Spam distribution")
    @JsonProperty("banReason")
    private String banReason;

    @Schema(example = "[USER, ADMIN]")
    @JsonProperty("roles")
    private Set<UserRole> roles;

    @Schema(example = "de")
    @JsonProperty("locale")
    private String locale;
}
