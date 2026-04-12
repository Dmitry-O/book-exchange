package com.example.bookexchange.support;

public final class TestReportStrings {

    private static final int MAX_LENGTH = 255;

    private TestReportStrings() {

    }

    public static String comment(String value) {
        return bounded(value);
    }

    public static String banReason(String value) {
        return bounded(value);
    }

    private static String bounded(String value) {
        return value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH);
    }
}
