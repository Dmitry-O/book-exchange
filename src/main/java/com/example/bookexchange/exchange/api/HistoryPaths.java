package com.example.bookexchange.exchange.api;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class HistoryPaths {

    public static final String HISTORY_PATH = "/history";
    public static final String HISTORY_PATH_EXCHANGE_ID = HISTORY_PATH + "/{exchangeId}";
}
