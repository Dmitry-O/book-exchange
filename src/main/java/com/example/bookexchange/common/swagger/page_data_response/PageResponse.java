package com.example.bookexchange.common.swagger.page_data_response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;

    @Schema(example = "0")
    private int page;

    @Schema(example = "1")
    private int size;

    @Schema(example = "1")
    private long totalElements;

    @Schema(example = "1")
    private int totalPages;
}
