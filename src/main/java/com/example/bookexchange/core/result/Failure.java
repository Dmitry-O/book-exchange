package com.example.bookexchange.core.result;

import com.example.bookexchange.models.MessageKey;
import org.springframework.http.HttpStatus;

import java.util.function.Function;

public record Failure<T>(
        MessageKey messageKey,
        HttpStatus status,
        Object... args
) implements Result<T> {

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public <R> Result<R> map(Function<T, R> mapper) {
        return new Failure<>(messageKey, status, args);
    }

    @Override
    public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
        return new Failure<>(messageKey, status, args);
    }
}
