package com.example.bookexchange.book.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum BookCategoryDTO {
    ACTION_ADVENTURE("Action & Adventure"),
    ART_DESIGN("Art & Design"),
    AUTOBIOGRAPHY("Autobiography"),
    BIOGRAPHY("Biography"),
    BUSINESS("Business"),
    CHILDREN("Children"),
    CLASSIC("Classic"),
    COMICS("Comics"),
    CONTEMPORARY("Contemporary"),
    COOKING("Cooking"),
    CRIME("Crime"),
    DRAMA("Drama"),
    EDUCATION("Education"),
    FANTASY("Fantasy"),
    GRAPHIC_NOVEL("Graphic Novel"),
    HEALTH("Health"),
    HISTORY("History"),
    HORROR("Horror"),
    MANGA("Manga"),
    MEMOIR("Memoir"),
    MYSTERY("Mystery"),
    NON_FICTION("Non-fiction"),
    NOVEL("Novel"),
    PHILOSOPHY("Philosophy"),
    POETRY("Poetry"),
    PSYCHOLOGY("Psychology"),
    RELIGION("Religion"),
    ROMANCE("Romance"),
    SCIENCE("Science"),
    SCIENCE_FICTION("Science Fiction"),
    SELF_HELP("Self-help"),
    TECHNOLOGY("Technology"),
    THRILLER("Thriller"),
    TRAVEL("Travel"),
    YOUNG_ADULT("Young Adult"),
    OTHER("Other");

    private final String property;

    BookCategoryDTO(String property) {
        this.property = property;
    }

    @JsonValue
    public String getProperty() {
        return property;
    }

    @JsonCreator
    public static BookCategoryDTO fromProperty(String value) {
        return Arrays.stream(values())
                .filter(category -> category.property.equalsIgnoreCase(value) || category.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported book category: " + value));
    }

    public static BookCategoryDTO fromStorageValue(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }

        return Arrays.stream(values())
                .filter(category -> category.property.equalsIgnoreCase(value) || category.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(OTHER);
    }
}
