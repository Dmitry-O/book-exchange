package com.example.bookexchange.exception;

import org.springframework.http.HttpStatus;

public class EntityExistsException extends ApiException {

    public EntityExistsException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
