package com.example.bookexchange.common.i18n;

public interface MessageService {

    String getMessage(MessageKey messageKey, Object... args);

    String getMessage(String messageKey);
}
