package com.example.bookexchange.common.city.dto;

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
public class CityAutocompleteDTO {

    @Schema(example = "Munich")
    @JsonProperty("value")
    private String value;

    @Schema(example = "München")
    @JsonProperty("label")
    private String label;
}
