package com.example.bookexchange.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportTargetUserDTO {

    @Schema(example = "42")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "user_12345")
    @JsonProperty("nickname")
    private String nickname;

    @Schema(example = "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/profile_photo_1712582410000.jpg")
    @JsonProperty("photoUrl")
    private String photoUrl;
}
