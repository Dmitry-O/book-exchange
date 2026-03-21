package com.example.bookexchange.core.result;

import com.example.bookexchange.models.MessageKey;
import org.springframework.http.HttpStatus;

import java.util.function.Function;

public record Success<T>(
        T body,
        HttpStatus status,
        MessageKey messageKey,
        String eTag
) implements Result<T> {

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public <R> Result<R> map(Function<T, R> mapper) {
        return new Success<>(mapper.apply(body), status, messageKey, eTag);
    }

    @Override
    public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
        return mapper.apply(body);
    }
}
