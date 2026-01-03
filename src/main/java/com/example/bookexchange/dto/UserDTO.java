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

@JsonDeserialize(builder = UserDTO.UserDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    @JsonProperty("id")
    @NotNull
    private Long id;

    @JsonProperty("email")
    @NotBlank
    @NotNull
    @Email
    private String email;

    @JsonProperty("nickname")
    @NotBlank
    @NotNull
    @Size(min = 5, max = 20)
    private String nickname;

    @JsonProperty("photoBase64")
    private String photoBase64;
}
