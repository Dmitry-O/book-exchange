package com.example.bookexchange.exception;

import com.example.bookexchange.models.MessageKey;
import org.springframework.http.HttpStatus;

public class EntityExistsException extends ApiException {

    public EntityExistsException(MessageKey messageKey, Object... args) {
        super(HttpStatus.CONFLICT, messageKey, args);
    }
}
