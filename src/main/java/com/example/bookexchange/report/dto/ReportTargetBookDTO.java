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
public class ReportTargetBookDTO {

    @Schema(example = "15")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "Charley Smash")
    @JsonProperty("name")
    private String name;

    @Schema(example = "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/books/15_1712582410000.jpg")
    @JsonProperty("photoUrl")
    private String photoUrl;

    @Schema(example = "42")
    @JsonProperty("ownerUserId")
    private Long ownerUserId;

    @Schema(example = "user_12345")
    @JsonProperty("ownerNickname")
    private String ownerNickname;

    @Schema(example = "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/profile_photo_1712582410000.jpg")
    @JsonProperty("ownerPhotoUrl")
    private String ownerPhotoUrl;
}
