package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = BookUpdateDTO.BookUpdateDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookUpdateDTO {

    @JsonProperty("name")
    @Size(min = 3, max = 25)
    private String name;

    @JsonProperty("description")
    @Size(min = 3, max = 255)
    private String description;

    @JsonProperty("author")
    @Size(min = 3, max = 25)
    private String author;

    @JsonProperty("category")
    @Size(min = 3, max = 20)
    private String category;

    @JsonProperty("publicationYear")
    private Integer publicationYear;

    @JsonProperty("photoBase64")
    private String photoBase64;

    @JsonProperty("city")
    @Size(min = 3, max = 25)
    private String city;

    @JsonProperty("contactDetails")
    private String contactDetails;

    @JsonProperty("isGift")
    private Boolean isGift;
}
