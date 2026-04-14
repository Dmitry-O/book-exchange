package com.example.bookexchange.support;

public final class TestBookStrings {

    private TestBookStrings() {
    }

    public static String name(int number) {
        return numbered("Book ", number, 25);
    }

    public static String description(int number) {
        return numbered("Book description ", number, 255);
    }

    public static String author(int number) {
        return numbered("Author ", number, 25);
    }

    public static String city(int number) {
        return numbered("City ", number, 25);
    }

    public static String contactDetails(int number) {
        return numbered("Contact Details ", number, 255);
    }

    public static String updatedName(int number) {
        return numbered("Updated Book ", number, 25);
    }

    public static String updatedDescription(int number) {
        return numbered("Updated description ", number, 255);
    }

    public static String updatedAuthor(int number) {
        return numbered("Updated Author ", number, 25);
    }

    public static String updatedCity(int number) {
        return numbered("Updated City ", number, 25);
    }

    public static String updatedContactDetails(int number) {
        return numbered("Updated Contact Details ", number, 255);
    }

    private static String numbered(String prefix, int number, int maxLength) {
        String value = prefix + number;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
