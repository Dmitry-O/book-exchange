package com.example.bookexchange.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BookSearchDTO {

    private String author;
    private String category;
    private String city;
    private Integer publicationYear;
    private Boolean isGift;
    private String searchText;
    private String sortBy;
    private String sortDirection;
}
