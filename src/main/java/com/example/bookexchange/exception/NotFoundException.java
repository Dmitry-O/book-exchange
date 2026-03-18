package com.example.bookexchange.exception;

import com.example.bookexchange.models.MessageKey;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NotFoundException extends ApiException {

    public NotFoundException(MessageKey messageKey, Object... args) {
        super(HttpStatus.NOT_FOUND, messageKey, args);
    }
}
