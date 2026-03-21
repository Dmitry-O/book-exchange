package com.example.bookexchange.core.result;

import com.example.bookexchange.core.error.ApiErrorDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private ApiErrorDTO error;
}
