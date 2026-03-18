package com.example.bookexchange.services;

import com.example.bookexchange.models.MessageKey;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@RequiredArgsConstructor
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageSource messageSource;

    @Override
    public String getMessage(MessageKey messageKey, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();

        return messageSource.getMessage(messageKey.key(), args, locale);
    }

    @Override
    public String getMessage(String messageKey) {
        Locale locale = LocaleContextHolder.getLocale();

        return messageSource.getMessage(messageKey, null, locale);
    }
}
