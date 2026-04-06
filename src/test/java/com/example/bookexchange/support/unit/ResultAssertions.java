package com.example.bookexchange.support.unit;

import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Failure;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.Success;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

public final class ResultAssertions {

    private ResultAssertions() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Failure<T> assertFailure(Result<T> result, MessageKey messageKey, HttpStatus status) {
        assertThat(result).isInstanceOf(Failure.class);

        Failure<T> failure = (Failure<T>) result;
        assertThat(failure.messageKey()).isEqualTo(messageKey);
        assertThat(failure.status()).isEqualTo(status);

        return failure;
    }

    @SuppressWarnings("unchecked")
    public static <T> Success<T> assertSuccess(Result<T> result, HttpStatus status) {
        assertThat(result).isInstanceOf(Success.class);

        Success<T> success = (Success<T>) result;
        assertThat(success.status()).isEqualTo(status);

        return success;
    }

    public static <T> Success<T> assertSuccess(Result<T> result, HttpStatus status, MessageKey messageKey) {
        Success<T> success = assertSuccess(result, status);
        assertThat(success.messageKey()).isEqualTo(messageKey);
        return success;
    }
}
