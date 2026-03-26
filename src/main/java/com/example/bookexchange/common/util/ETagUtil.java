package com.example.bookexchange.common.util;

import com.example.bookexchange.common.audit.model.VersionedEntity;

public class ETagUtil {

    private ETagUtil() {}

    public static String form(VersionedEntity entity) {
        return String.valueOf(entity.getVersion());
    }
}
