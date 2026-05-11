package com.example.bookexchange.common.email;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationEmailRequest {

    private final String emailTo;
    private final String recipientName;
    private final String locale;
    private final String subject;
    private final String preheader;
    private final String eyebrow;
    private final String title;
    private final String intro;
    private final String summary;
    private final String eventTime;
    private final String ctaLabel;
    private final String ctaUrl;

    @Builder.Default
    private final List<NotificationEmailBadge> highlights = List.of();

    private final NotificationEmailExchange exchange;

    private final NotificationEmailBook book;

    private final NotificationEmailUserCard user;

    private final NotificationEmailReport report;

    @Builder.Default
    private final List<NotificationEmailDetail> details = List.of();
}
