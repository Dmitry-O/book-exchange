package com.example.bookexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookDTO {

    private Long id;
    private String name;
    private String description;
    private String author;
    private String category;
    private Integer publicationYear;
    private String photoBase64;
    private String city;
    private Boolean isGift;
    private Boolean isExchanged;
}
