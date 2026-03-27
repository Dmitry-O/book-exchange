package com.example.bookexchange.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQueryDTO {

    @Schema(description = "Page index, starts from 0", example = "0")
    @Min(0)
    @JsonProperty("pageIndex")
    private Integer pageIndex = 0;

    @Schema(description = "Page size", example = "20")
    @Min(1) @Max(100)
    @JsonProperty("pageSize")
    private Integer pageSize = 20;
}
