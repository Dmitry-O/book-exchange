package com.example.bookexchange.exception;

import com.example.bookexchange.models.MessageKey;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final MessageKey messageKey;
    private final Object[] args;

    protected ApiException(HttpStatus status, MessageKey messageKey, Object... args) {
        this.status = status;
        this.messageKey = messageKey;
        this.args = args;
    }
}
