package com.example.bookexchange.dto;

import com.example.bookexchange.models.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("id")
    private Long id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("nickname")
    private String nickname;

    @JsonProperty("photoBase64")
    private String photoBase64;

    @JsonProperty("bannedUntil")
    private String bannedUntil;

    @JsonProperty("bannedPermanently")
    private String bannedPermanently;

    @JsonProperty("banReason")
    private String banReason;

    @JsonProperty("roles")
    private Set<UserRole> roles;
}
