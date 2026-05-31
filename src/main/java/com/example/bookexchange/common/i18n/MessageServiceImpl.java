package com.example.bookexchange.common.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class MessageServiceImpl implements MessageService {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Berlin");

    private final MessageSource messageSource;

    @Override
    public String getMessage(MessageKey messageKey, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();

        return messageSource.getMessage(messageKey.key(), formatArgs(locale, args), locale);
    }

    @Override
    public String getMessage(String messageKey) {
        Locale locale = LocaleContextHolder.getLocale();

        return messageSource.getMessage(messageKey, null, locale);
    }

    private Object[] formatArgs(Locale locale, Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }

        Object[] formattedArgs = new Object[args.length];

        for (int index = 0; index < args.length; index++) {
            formattedArgs[index] = args[index] instanceof Instant instant
                    ? formatInstant(instant, locale)
                    : args[index];
        }

        return formattedArgs;
    }

    private String formatInstant(Instant instant, Locale locale) {
        ZonedDateTime dateTime = instant.atZone(DISPLAY_ZONE);
        boolean sameYear = dateTime.getYear() == ZonedDateTime.now(DISPLAY_ZONE).getYear();
        String pattern = sameYear ? "d MMM, HH:mm" : "d MMM yyyy, HH:mm";

        return DateTimeFormatter.ofPattern(pattern, locale)
                .format(dateTime);
    }
}
