package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = BookSearchDTO.BookSearchDTOBuilder.class)
@Builder
@Data
public class BookSearchDTO {

    @JsonProperty("author")
    @Size(min = 3, max = 25)
    private String author;

    @JsonProperty("category")
    @Size(min = 3, max = 20)
    private String category;

    @JsonProperty("city")
    @Size(min = 3, max = 25)
    private String city;

    @JsonProperty("publicationYear")
    private Integer publicationYear;

    @JsonProperty("isGift")
    private Boolean isGift;

    @JsonProperty("searchText")
    @Size(min = 3, max = 25)
    private String searchText;

    @JsonProperty("sortBy")
    private String sortBy;

    @JsonProperty("sortDirection")
    private String sortDirection;
}
