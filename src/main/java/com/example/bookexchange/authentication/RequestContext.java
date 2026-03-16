package com.example.bookexchange.authentication;

public class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
