package com.example.bookexchange.security.context;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class RequestContextHolder {

    private static final ThreadLocal<Boolean> INCLUDE_DELETED = ThreadLocal.withInitial(() -> false);

    public static boolean isIncludeDeleted() {
        return Boolean.TRUE.equals(INCLUDE_DELETED.get());
    }

    public static void setIncludeDeleted(boolean includeDeleted) {
        INCLUDE_DELETED.set(includeDeleted);
    }

    public static void clear() {
        INCLUDE_DELETED.remove();
    }
}
