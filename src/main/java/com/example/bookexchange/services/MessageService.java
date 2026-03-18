package com.example.bookexchange.services;

import com.example.bookexchange.models.MessageKey;

public interface MessageService {

    String getMessage(MessageKey messageKey, Object... args);

    String getMessage(String messageKey);
}
