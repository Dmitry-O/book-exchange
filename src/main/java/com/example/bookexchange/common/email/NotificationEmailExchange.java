package com.example.bookexchange.common.email;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationEmailExchange {

    private final String exchangeId;
    private final String referenceText;
    private final NotificationEmailBadge status;
    private final String changedAt;
    private final NotificationEmailBook leftBook;
    private final NotificationEmailBook rightBook;
    private final NotificationEmailUserCard leftUser;
    private final NotificationEmailUserCard rightUser;
}
