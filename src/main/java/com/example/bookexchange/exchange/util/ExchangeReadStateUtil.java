package com.example.bookexchange.exchange.util;

import com.example.bookexchange.exchange.model.Exchange;

public final class ExchangeReadStateUtil {

    private ExchangeReadStateUtil() {
    }

    public static void markCreatedBySender(Exchange exchange) {
        markReadBySender(exchange);
        markUnreadByReceiver(exchange);
    }

    public static void markUpdatedBySender(Exchange exchange) {
        markReadBySender(exchange);
        markUnreadByReceiver(exchange);
    }

    public static void markUpdatedByReceiver(Exchange exchange) {
        markUnreadBySender(exchange);
        markReadByReceiver(exchange);
    }

    public static void markUpdatedForBoth(Exchange exchange) {
        markUnreadBySender(exchange);
        markUnreadByReceiver(exchange);
    }

    public static void markReadBySender(Exchange exchange) {
        exchange.setIsReadBySender(Boolean.TRUE);
    }

    public static void markReadByReceiver(Exchange exchange) {
        exchange.setIsReadByReceiver(Boolean.TRUE);
    }

    public static void markUnreadBySender(Exchange exchange) {
        exchange.setIsReadBySender(Boolean.FALSE);
    }

    public static void markUnreadByReceiver(Exchange exchange) {
        exchange.setIsReadByReceiver(Boolean.FALSE);
    }
}
