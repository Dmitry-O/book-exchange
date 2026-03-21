package com.example.bookexchange.util;

import com.example.bookexchange.models.VersionedEntity;

public class ETagUtil {

    private ETagUtil() {}

    public static String form(VersionedEntity entity) {
        return String.valueOf(entity.getVersion());
    }
}
