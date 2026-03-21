package com.example.bookexchange.core.result;

import com.example.bookexchange.models.MessageKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;

import java.util.Optional;

public final class ResultFactory {

    private ResultFactory() {}

    public static <T> Result<T> ok(T body) {
        return new Success<>(body, HttpStatus.OK, null, null);
    }

    public static <T> Result<T> okETag(T body, String eTag) {
        return new Success<>(body, HttpStatus.OK, null, eTag);
    }

    public static Result<Void> okMessage(MessageKey messageKey, Object... args) {
        return new Success<>(null, HttpStatus.OK, messageKey, null);
    }

    public static <T> Result<T> created(T body, MessageKey messageKey, String eTag) {
        return new Success<>(body, HttpStatus.CREATED, messageKey, eTag);
    }

    public static <T> Result<T> updated(T body, MessageKey messageKey, String eTag, Object... args) {
        return new Success<>(body, HttpStatus.OK, messageKey, eTag);
    }

    public static <T> Result<T> successVoid() {
        return new Success<>(null, null, null, null);
    }

    public static <T> Result<T> notFound(MessageKey messageKey, Object... args) {
        return new Failure<>(messageKey, HttpStatus.NOT_FOUND, args);
    }

    public static <T> Result<T> entityExists(MessageKey messageKey, Object... args) {
        return new Failure<>(messageKey, HttpStatus.CONFLICT, args);
    }

    public static <T> Result<T> error(MessageKey messageKey, HttpStatus status, Object... args) {
        return new Failure<>(messageKey, status, args);
    }

    public static <T, ID> Result<T> fromRepository(
            JpaRepository<T, ID> repo,
            ID id,
            MessageKey messageKey
    ) {
        return repo.findById(id)
                .map(ResultFactory::ok)
                .orElseGet(() ->
                        ResultFactory.error(
                                messageKey,
                                HttpStatus.NOT_FOUND,
                                id
                        )
                );
    }

    public static <T> Result<T> fromOptional(
            Optional<T> optional,
            MessageKey error,
            Object... args
    ) {
        return optional
                .map(ResultFactory::ok)
                .orElseGet(() -> ResultFactory.notFound(error, args));
    }
}