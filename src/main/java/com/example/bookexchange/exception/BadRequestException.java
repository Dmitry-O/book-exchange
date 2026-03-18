package com.example.bookexchange.exception;

import com.example.bookexchange.models.MessageKey;
import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(MessageKey messageKey, Object... args) {
        super(HttpStatus.BAD_REQUEST, messageKey, args);
    }
}
