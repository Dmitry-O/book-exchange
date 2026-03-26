package com.example.bookexchange.book.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@Schema(description = "Book response DTO")
@JsonDeserialize(builder = BookDTO.BookDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookDTO {

    @Schema(example = "1")
    @JsonProperty("id")
    private Long id;

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

    @Schema(example = "true")
    @JsonProperty("isGift")
    private Boolean isGift;

    @Schema(example = "false")
    @JsonProperty("isExchanged")
    private Boolean isExchanged;
}
