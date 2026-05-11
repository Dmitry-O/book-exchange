package com.example.bookexchange.common.email;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationEmailUserCard {

    private final String title;
    private final String name;
    private final String meta;
    private final String photoUrl;
    private final String initial;
    private final NotificationEmailBadge status;
}
