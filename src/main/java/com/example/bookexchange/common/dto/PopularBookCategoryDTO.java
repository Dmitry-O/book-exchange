package com.example.bookexchange.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PopularBookCategoryDTO {

    @Schema(example = "Fantasy")
    @JsonProperty("category")
    private String category;

    @Schema(example = "12")
    @JsonProperty("books")
    private Long books;
}
