package com.example.bookexchange.support;

public final class TestUserStrings {

    private TestUserStrings() {
    }

    public static String email(int number) {
        return "user" + number + "@test.com";
    }

    public static String nickname(int number) {
        return numbered("user", number, 20);
    }

    public static String updatedNickname(int number) {
        return numbered("updated_user_", number, 20);
    }

    private static String numbered(String prefix, int number, int maxLength) {
        String value = prefix + number;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
