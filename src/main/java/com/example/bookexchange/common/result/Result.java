package com.example.bookexchange.common.result;

import java.util.function.Function;

public sealed interface Result<T> permits Success, Failure {

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    <U> Result<U> map(Function<T, U> mapper);

    <U> Result<U> flatMap(Function<T, Result<U>> mapper);
}
