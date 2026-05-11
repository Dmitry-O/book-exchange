package com.example.bookexchange.common.email;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationEmailDetail {

    private final String label;
    private final String value;
}
