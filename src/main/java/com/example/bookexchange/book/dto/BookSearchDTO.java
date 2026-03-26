package com.example.bookexchange.book.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = BookSearchDTO.BookSearchDTOBuilder.class)
@Builder
@Data
public class BookSearchDTO {

    @Schema(example = "Frank Oester")
    @JsonProperty("author")
    @Size(min = 3, max = 25)
    private String author;

    @Schema(example = "Drama")
    @JsonProperty("category")
    @Size(min = 3, max = 20)
    private String category;

    @Schema(example = "London")
    @JsonProperty("city")
    @Size(min = 3, max = 25)
    private String city;

    @Schema(example = "1765")
    @JsonProperty("publicationYear")
    private Integer publicationYear;

    @Schema(example = "true")
    @JsonProperty("isGift")
    private Boolean isGift;

    @Schema(example = "Charley Smash")
    @JsonProperty("searchText")
    @Size(min = 3, max = 25)
    private String searchText;

    @Schema(example = "category")
    @JsonProperty("sortBy")
    private String sortBy;

    @Schema(example = "true")
    @JsonProperty("sortDirection")
    private String sortDirection;
}
