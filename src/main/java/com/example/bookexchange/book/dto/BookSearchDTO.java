package com.example.bookexchange.book.dto;

import com.example.bookexchange.common.dto.SortDirectionDTO;
import com.example.bookexchange.common.validation.ValidPublicationYear;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
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
    @ValidPublicationYear
    private Integer publicationYear;

    @Schema(example = "true")
    @JsonProperty("isGift")
    private Boolean isGift;

    @Schema(example = "Charley Smash")
    @JsonProperty("searchText")
    @Size(min = 3, max = 25)
    private String searchText;

    @Schema(example = "CATEGORY")
    @JsonProperty("sortBy")
    private BookSortFieldDTO sortBy;

    @Schema(example = "ASC")
    @JsonProperty("sortDirection")
    private SortDirectionDTO sortDirection;
}
