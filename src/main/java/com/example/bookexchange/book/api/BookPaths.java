package com.example.bookexchange.book.api;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class BookPaths {

    public static final String BOOK_PATH = "/book";
    public static final String BOOK_PATH_BOOK_ID = BOOK_PATH + "/{bookId}";
    public static final String BOOK_PATH_USER = BOOK_PATH + "/user";
    public static final String BOOK_PATH_HISTORY = BOOK_PATH + "/history";
    public static final String BOOK_PATH_USER_BOOK_ID = BOOK_PATH_USER + "/{bookId}";
    public static final String BOOK_PATH_USER_BOOK_ID_PHOTO = BOOK_PATH_USER_BOOK_ID + "/photo";
    public static final String BOOK_PATH_SEARCH = BOOK_PATH + "/search";
}
