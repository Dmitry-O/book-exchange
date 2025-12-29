package com.example.bookexchange.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BookSearchDTO {

    @NotBlank
    @Size(min = 3, max = 25)
    private String author;

    @NotBlank
    @Size(min = 3, max = 20)
    private String category;

    @NotBlank
    @Size(min = 3, max = 25)
    private String city;

    private Integer publicationYear;

    private Boolean isGift;

    @NotBlank
    @Size(min = 3, max = 25)
    private String searchText;

    @NotBlank
    private String sortBy;

    @NotBlank
    private String sortDirection;
}
