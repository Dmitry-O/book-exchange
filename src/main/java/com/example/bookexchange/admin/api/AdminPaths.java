package com.example.bookexchange.admin.api;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AdminPaths {

    public static final String ADMIN_PATH = "/admin";

    public static final String ADMIN_PATH_USERS = ADMIN_PATH + "/users";
    public static final String ADMIN_PATH_USERS_ID = ADMIN_PATH_USERS + "/{userId}";
    public static final String ADMIN_PATH_USERS_ID_GIVE_ADMIN_RIGHTS = ADMIN_PATH_USERS_ID + "/make-admin";
    public static final String ADMIN_PATH_USERS_ID_REVOKE_ADMIN_RIGHTS = ADMIN_PATH_USERS_ID + "/remove-admin";
    public static final String ADMIN_PATH_USERS_ID_BAN = ADMIN_PATH_USERS_ID + "/ban";
    public static final String ADMIN_PATH_USERS_ID_UNBAN = ADMIN_PATH_USERS_ID + "/unban";

    public static final String ADMIN_PATH_BOOKS = ADMIN_PATH + "/books";
    public static final String ADMIN_PATH_BOOKS_SEARCH = ADMIN_PATH_BOOKS + "/search";
    public static final String ADMIN_PATH_BOOKS_SEARCH_REINDEX = ADMIN_PATH_BOOKS + "/search/reindex";
    public static final String ADMIN_PATH_BOOKS_ID = ADMIN_PATH_BOOKS + "/{bookId}";
    public static final String ADMIN_PATH_BOOKS_ID_PHOTO = ADMIN_PATH_BOOKS_ID + "/photo";
    public static final String ADMIN_PATH_BOOKS_ID_RESTORE = ADMIN_PATH_BOOKS_ID + "/restore";

    public static final String ADMIN_PATH_EXCHANGES = ADMIN_PATH + "/exchanges";
    public static final String ADMIN_PATH_EXCHANGES_ID = ADMIN_PATH_EXCHANGES + "/{exchangeId}";

    public static final String ADMIN_PATH_REPORTS = ADMIN_PATH + "/reports";
    public static final String ADMIN_PATH_REPORTS_ID = ADMIN_PATH_REPORTS + "/{reportId}";
    public static final String ADMIN_PATH_REPORTS_ID_RESOLVE = ADMIN_PATH_REPORTS_ID + "/resolve";
    public static final String ADMIN_PATH_REPORTS_ID_REJECT = ADMIN_PATH_REPORTS_ID + "/reject";
}
