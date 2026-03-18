package com.example.bookexchange.models;

public enum MessageKey {

    AUTH_EMAIL_ALREADY_EXISTS("auth.email.already.exists"),
    AUTH_NICKNAME_ALREADY_EXISTS("auth.nickname.already.exists"),
    AUTH_ACCOUNT_REGISTERED("auth.account.registered"),
    AUTH_WRONG_EMAIL("auth.wrong.email"),
    AUTH_WRONG_PASSWORD("auth.wrong.password"),
    AUTH_ACCOUNT_NOT_VERIFIED("auth.account.not.verified"),
    AUTH_PERMANENTLY_BANNED("auth.permanently.banned"),
    AUTH_TEMPORARILY_BANNED("auth.temporarily.banned"),
    AUTH_WRONG_TOKEN("auth.wrong.token"),
    AUTH_REFRESH_TOKEN_EXPIRED("auth.refresh.token.expired"),
    AUTH_SAME_PASSWORDS("auth.same.passwords"),
    AUTH_WRONG_ACTUAL_PASSWORD("auth.wrong.actual.password"),
    AUTH_PASSWORD_CHANGED("auth.password.changed"),
    AUTH_TOKEN_EXPIRED("auth.token.expired"),
    AUTH_TOKEN_NOT_VALID("auth.token.not.valid"),
    AUTH_REGISTRATION_COMPLETED("auth.registration.completed"),
    AUTH_EMAIL_NOT_FOUND("auth.email.not.found"),
    AUTH_TOKEN_NOT_FOUND("auth.token.not.found"),
    AUTH_ACCOUNT_ALREADY_VERIFIED("auth.account.already.verified"),
    AUTH_LOGOUT("auth.logout"),

    USER_ACCOUNT_NOT_FOUND("user.not.found"),
    USER_RECEIVER_NOT_FOUND("user.receiver.not.found"),
    USER_PROFILE_UPDATED("user.profile.updated"),
    USER_ACCOUNT_DELETED("user.account.deleted"),

    BOOK_CREATED("book.created"),
    BOOK_DELETED("book.deleted"),
    BOOK_UPDATED("book.updated"),
    BOOK_NOT_FOUND("book.not.found"),
    BOOK_ALREADY_EXCHANGED("book.already.exchanged"),
    BOOK_ALREADY_IN_YOUR_LIST("book.already.in.your.list"),
    BOOK_EXCHANGE_ALREADY_EXISTS("book.exchange.already.exists"),
    BOOK_CANT_EXCHANGE_SAME_BOOK("book.cant.exchange.same.book"),
    BOOK_ALREADY_IN_RECEIVERS_LIST("book.already.in.receivers.list"),

    EXCHANGE_NOT_FOUND("exchange.not.found"),
    EXCHANGE_CANT_BE_APPROVED("exchange.cant.be.approved"),
    EXCHANGE_CANT_BE_DECLINED("exchange.cant.be.declined"),
    EXCHANGE_DECLINED("exchange.declined"),
    EXCHANGE_APPROVED("exchange.approved"),
    EXCHANGE_CANT_BE_WITH_YOURSELF("exchange.cant.be.with.yourself"),
    EXCHANGE_BETWEEN_BOOKS_EXISTS("exchange.between.books.exists"),
    EXCHANGE_CREATED("exchange.created"),

    EMAIL_VERIFY_ACCOUNT("email.verify.account"),
    EMAIL_RESET_PASSWORD("email.reset.password"),
    EMAIL_DELETE_ACCOUNT("email.delete.account"),

    REPORT_SENT("report.sent"),

    ADMIN_USER_NOT_FOUND("admin.user.not.found"),
    ADMIN_USER_ALREADY_ADMIN("admin.user.already.admin"),
    ADMIN_RIGHTS_GIVEN("admin.rights.given"),
    ADMIN_USER_NOT_ADMIN("admin.user.not.admin"),
    ADMIN_RIGHTS_REVOKED("admin.rights.revoked"),
    ADMIN_CANT_BAN_YOURSELF("admin.cant.ban.yourself"),
    ADMIN_REQUEST_NOT_VALID("admin.request.not.valid"),
    ADMIN_USER_BANNED("admin.user.banned"),
    ADMIN_USER_UNBANNED("admin.user.unbanned"),
    ADMIN_BOOK_NOT_FOUND("admin.book.not.found"),
    ADMIN_BOOK_DELETED("admin.book.deleted"),
    ADMIN_BOOK_UPDATED("admin.book.updated"),
    ADMIN_REPORT_NOT_FOUND("admin.report.not.found"),
    ADMIN_REPORT_RESOLVED("admin.report.resolved"),
    ADMIN_REPORT_REJECTED("admin.report.rejected"),
    ADMIN_BOOK_RESTORED("admin.book.restored"),
    ADMIN_EXCHANGE_NOT_FOUND("admin.exchange.not.found"),
    ADMIN_USER_DELETED("admin.user.deleted"),

    SYSTEM_WRONG_EMAIL_TYPE("system.wrong.email.type"),
    SYSTEM_USER_NOT_FOUND("system.user.not.found"),
    SYSTEM_ACCESS_FORBIDDEN("system.access.forbidden"),
    SYSTEM_UNEXPECTED_ERROR("system.unexpected.error"),
    SYSTEM_INVALID_TOKEN("system.invalid.token"),
    SYSTEM_TOO_MANY_REQUESTS("system.too.many.requests"),
    SYSTEM_UNEXPECTED_DB_ERROR("system.unexpected.db.error"),
    SYSTEM_OPTIMISTIC_LOCK("system.optimistic.lock"),
    SYSTEM_INVALID_DATA("system.invalid.data");

    private final String key;

    MessageKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
