package com.example.bookexchange.common.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

import java.time.Instant;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MessageServiceImplTest {

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void shouldFormatInstantArgsForUserFacingMessages() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage(
                MessageKey.AUTH_TEMPORARILY_BANNED.key(),
                Locale.ENGLISH,
                "The user has been banned until {0}. Reason for the ban: {1}"
        );
        MessageServiceImpl messageService = new MessageServiceImpl(messageSource);

        LocaleContextHolder.setLocale(Locale.ENGLISH);

        String message = messageService.getMessage(
                MessageKey.AUTH_TEMPORARILY_BANNED,
                Instant.parse("2026-05-30T14:05:00Z"),
                "Spam"
        );

        assertThat(message)
                .contains("30 May")
                .contains("16:05")
                .doesNotContain("2026-05-30T14:05:00Z");
    }
}
