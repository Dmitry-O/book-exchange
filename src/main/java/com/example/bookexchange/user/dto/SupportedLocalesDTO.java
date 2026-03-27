package com.example.bookexchange.user.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SupportedLocalesDTO {

    EN("en"),
    DE("de"),
    RU("ru");

    private final String property;

    SupportedLocalesDTO(String property) {
        this.property = property;
    }

    @JsonValue
    public String getValue() {
        return property;
    }

    @JsonCreator
    public static SupportedLocalesDTO fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (SupportedLocalesDTO locale : values()) {
            if (locale.property.equalsIgnoreCase(value)) {
                return locale;
            }
        }

        throw new IllegalArgumentException("Unsupported locale: " + value);
    }
}
