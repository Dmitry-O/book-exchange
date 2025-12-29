package com.example.bookexchange.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank
    @Size(min = 3, max = 25)
    private String name;

    @NotBlank
    @Size(min = 3, max = 255)
    private String description;

    @NotBlank
    @Size(min = 3, max = 25)
    private String author;

    @NotBlank
    @Size(min = 3, max = 20)
    private String category;

    private Integer publicationYear;

    private String photoBase64;

    @NotBlank
    @Size(min = 3, max = 25)
    private String city;

    @NotBlank
    private String contactDetails;

    private Boolean isGift;
}
