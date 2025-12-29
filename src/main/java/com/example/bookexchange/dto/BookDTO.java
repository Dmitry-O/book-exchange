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
public class BookDTO {

    @NotNull
    private Long id;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    private String name;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 255)
    private String description;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    private String author;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 20)
    private String category;

    @NotNull
    private Integer publicationYear;

    private String photoBase64;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    private String city;

    private Boolean isGift;

    private Boolean isExchanged;
}
