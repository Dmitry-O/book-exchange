package com.example.bookexchange.exception;

import com.example.bookexchange.models.MessageKey;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

    public ForbiddenException(MessageKey messageKey, Object... args) {
        super(HttpStatus.FORBIDDEN, messageKey, args);
    }
}
