package com.example.bookexchange.common.email;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationEmailBook {

    private final String title;
    private final String name;
    private final String subtitle;
    private final String meta;
    private final String photoUrl;
    private final NotificationEmailBadge status;
    private final boolean gift;
    private final boolean placeholder;
    private final String placeholderTitle;
    private final String placeholderText;
}
