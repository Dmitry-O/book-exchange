package com.example.bookexchange.common.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "ApiResponse")
public class ApiResponse<T> {

    @Schema(example = "true")
    private boolean success;

    @Schema(nullable = true)
    private T data;

    @Schema(nullable = true, example = "Operation successful")
    private String message;

    @Schema(nullable = true, example = "null")
    private ApiError error;
}