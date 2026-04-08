package com.example.bookexchange.book.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Book response DTO")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(example = "3")
    @JsonProperty("version")
    private Long version;

    @Schema(example = "42")
    @JsonProperty("ownerUserId")
    private Long ownerUserId;

    @Schema(example = "user_12345")
    @JsonProperty("ownerNickname")
    private String ownerNickname;

    @Schema(example = "Charley Smash")
    @JsonProperty("name")
    private String name;

    @Schema(example = "An interesting book about ...")
    @JsonProperty("description")
    private String description;

    @Schema(example = "Frank Oester")
    @JsonProperty("author")
    private String author;

    @Schema(example = "Drama")
    @JsonProperty("category")
    private String category;

    @Schema(example = "1765")
    @JsonProperty("publicationYear")
    private Integer publicationYear;

    @Schema(example = "Book photo")
    @JsonProperty("photoBase64")
    private String photoBase64;

    @Schema(example = "London")
    @JsonProperty("city")
    private String city;

    @Schema(example = "Juchostr. 12, 44320 Dortmund; 01512 0089 34 567")
    @JsonProperty("contactDetails")
    private String contactDetails;

    @Schema(example = "true")
    @JsonProperty("isGift")
    private Boolean isGift;

    @Schema(example = "false")
    @JsonProperty("isExchanged")
    private Boolean isExchanged;
}
