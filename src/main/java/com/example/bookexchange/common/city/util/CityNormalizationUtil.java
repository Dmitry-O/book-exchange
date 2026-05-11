package com.example.bookexchange.common.city.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CityNormalizationUtil {

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return Normalizer.normalize(value.trim(), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
