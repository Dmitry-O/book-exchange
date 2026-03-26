package com.example.bookexchange.exchange.api;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class RequestPaths {

    public static final String REQUEST_PATH = "/request";
    public static final String EXCHANGE_ID_PATH = "/{exchangeId}";
    public static final String REQUEST_PATH_EXCHANGE_ID = REQUEST_PATH + EXCHANGE_ID_PATH;
    public static final String REQUEST_PATH_DECLINE_REQUEST = REQUEST_PATH + EXCHANGE_ID_PATH + "/decline";
}
