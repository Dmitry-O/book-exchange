package com.example.bookexchange.user.api;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class UserPaths {

    public static final String USER_PATH = "/user";
    public static final String USER_PATH_PHOTO = USER_PATH + "/photo";
    public static final String USER_PATH_RESET_PASSWORD = USER_PATH + "/reset_password";
    public static final String USER_PATH_LOGOUT = USER_PATH + "/logout";
}
