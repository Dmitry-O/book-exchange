package com.example.bookexchange.auth.api;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AuthPaths {

    public static final String AUTH_PATH = "/auth";
    public static final String AUTH_PATH_REGISTER = AUTH_PATH + "/register";
    public static final String AUTH_PATH_LOGIN = AUTH_PATH + "/login";
    public static final String AUTH_PATH_REFRESH_TOKEN = AUTH_PATH + "/refresh_token";
    public static final String AUTH_PATH_CONFIRM_REGISTRATION = AUTH_PATH + "/verify";
    public static final String AUTH_PATH_FORGOT_PASSWORD = AUTH_PATH + "/forgot_password";
    public static final String AUTH_PATH_RESET_PASSWORD = AUTH_PATH + "/reset_password";
    public static final String AUTH_PATH_RESEND_CONFIRMATION_EMAIL = AUTH_PATH + "/resend_confirmation_email";
    public static final String AUTH_PATH_INITIATE_DELETE_ACCOUNT = AUTH_PATH + "/initiate_delete_account";
    public static final String AUTH_PATH_DELETE_ACCOUNT = AUTH_PATH + "/delete_account";
}
