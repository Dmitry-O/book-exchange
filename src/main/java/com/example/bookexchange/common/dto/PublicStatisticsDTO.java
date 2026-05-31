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
public class PublicStatisticsDTO {

    @Schema(example = "128")
    @JsonProperty("users")
    private long users;

    @Schema(example = "842")
    @JsonProperty("books")
    private long books;

    @Schema(example = "216")
    @JsonProperty("exchanges")
    private long exchanges;
}
