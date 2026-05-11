package com.example.bookexchange.common.email;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationEmailBadge {

    private final String label;
    private final String value;
    private final String tone;
}
