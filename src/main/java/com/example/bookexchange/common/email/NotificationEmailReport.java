package com.example.bookexchange.common.email;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationEmailReport {

    private final String title;
    private final NotificationEmailBadge status;
    private final String targetText;
    private final String targetStateText;
    private final String targetModerationText;
    private final NotificationEmailBook targetBook;
    private final NotificationEmailUserCard targetUser;
    private final String reason;
    private final String comment;
}
