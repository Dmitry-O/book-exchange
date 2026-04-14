package com.example.bookexchange.book.dto;

import com.example.bookexchange.common.validation.Base64Image;
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
public class BookUpdateDTO {

    @Schema(example = "Charley Smash")
    @JsonProperty("name")
    @Size(min = 3, max = 25)
    private String name;

    @Schema(example = "An interesting book about ...")
    @JsonProperty("description")
    @Size(min = 3, max = 255)
    private String description;

    @Schema(example = "Frank Oester")
    @JsonProperty("author")
    @Size(min = 3, max = 25)
    private String author;

    @Schema(example = "Drama")
    @JsonProperty("category")
    private BookCategoryDTO category;

    @Schema(example = "1765")
    @JsonProperty("publicationYear")
    @ValidPublicationYear
    private Integer publicationYear;

    @Schema(example = "Book photo")
    @JsonProperty("photoBase64")
    @Base64Image
    private String photoBase64;

    @Schema(example = "London")
    @JsonProperty("city")
    @Size(min = 3, max = 25)
    private String city;

    @Schema(example = "Juchostr. 12, 44320 Dortmund; 01512 0089 34 567")
    @JsonProperty("contactDetails")
    @Size(min = 3, max = 255)
    private String contactDetails;

    @Schema(example = "true")
    @JsonProperty("isGift")
    private Boolean isGift;
}
