package com.example.bookexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookCreateDTO {

    private String name;
    private String description;
    private String author;
    private String category;
    private Integer publicationYear;
    private String photoBase64;
    private String city;
    private String contactDetails;
    private Boolean isGift;
}
