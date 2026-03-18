package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = BookDTO.BookDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("author")
    private String author;

    @JsonProperty("category")
    private String category;

    @JsonProperty("publicationYear")
    private Integer publicationYear;

    @JsonProperty("photoBase64")
    private String photoBase64;

    @JsonProperty("city")
    private String city;

    @JsonProperty("isGift")
    private Boolean isGift;

    @JsonProperty("isExchanged")
    private Boolean isExchanged;
}
