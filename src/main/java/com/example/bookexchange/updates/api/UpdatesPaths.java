package com.example.bookexchange.updates.api;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class UpdatesPaths {

    public static final String UPDATES_PATH = "/updates";
    public static final String UPDATES_PATH_UNREAD = UPDATES_PATH + "/unread";
    public static final String UPDATES_PATH_MARK_ALL_READ = UPDATES_PATH + "/read-state/all";
    public static final String UPDATES_PATH_EXCHANGE_ID_READ_STATE = UPDATES_PATH + "/{exchangeId}/read-state";
    public static final String UPDATES_PATH_NOTIFICATION_ID_READ_STATE = UPDATES_PATH + "/notifications/{notificationId}/read-state";
}
