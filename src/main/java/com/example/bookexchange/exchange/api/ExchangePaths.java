package com.example.bookexchange.exchange.api;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ExchangePaths {

    public static final String REQUEST_PATH = "/request";
    public static final String EXCHANGE_ID_PATH = "/{exchangeId}";
    public static final String REQUEST_PATH_EXCHANGE_ID = REQUEST_PATH + EXCHANGE_ID_PATH;
    public static final String REQUEST_PATH_DECLINE_REQUEST = REQUEST_PATH + EXCHANGE_ID_PATH + "/decline";

    public static final String OFFER_PATH = "/offer";
    public static final String OFFER_PATH_EXCHANGE_ID = OFFER_PATH + EXCHANGE_ID_PATH;
    public static final String OFFER_PATH_APPROVE_OFFER = OFFER_PATH + EXCHANGE_ID_PATH + "/approve";
    public static final String OFFER_PATH_DECLINE_OFFER = OFFER_PATH + EXCHANGE_ID_PATH + "/decline";

    public static final String HISTORY_PATH = "/history";
    public static final String HISTORY_PATH_EXCHANGE_ID = HISTORY_PATH + "/{exchangeId}";
}
