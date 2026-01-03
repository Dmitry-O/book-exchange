package com.example.bookexchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = BookCreateDTO.BookCreateDTOBuilder.class)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookCreateDTO {

    @JsonProperty("name")
    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    private String name;

    @JsonProperty("description")
    @NotBlank
    @NotNull
    @Size(min = 3, max = 255)
    private String description;

    @JsonProperty("author")
    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    private String author;

    @JsonProperty("category")
    @NotBlank
    @NotNull
    @Size(min = 3, max = 20)
    private String category;

    @JsonProperty("publicationYear")
    @NotNull
    private Integer publicationYear;

    @JsonProperty("photoBase64")
    private String photoBase64;

    @JsonProperty("city")
    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    private String city;

    @JsonProperty("contactDetails")
    @NotBlank
    @NotNull
    private String contactDetails;

    @JsonProperty("isGift")
    private Boolean isGift = Boolean.FALSE;

    @JsonProperty("isExchanged")
    private Boolean isExchanged = Boolean.FALSE;
}
