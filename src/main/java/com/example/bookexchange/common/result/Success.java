package com.example.bookexchange.common.result;

import com.example.bookexchange.common.i18n.MessageKey;
import org.springframework.http.HttpStatus;

import java.util.function.Function;

public record Success<T>(
        T body,
        HttpStatus status,
        MessageKey messageKey,
        String eTag,
        Object... args
) implements Result<T> {

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public <R> Result<R> map(Function<T, R> mapper) {
        return new Success<>(mapper.apply(body), status, messageKey, eTag, args);
    }

    @Override
    public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
        return mapper.apply(body);
    }
}
