package com.example.bookexchange.common.demoemail;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DemoEmailSandboxPaths {

    public static final String DEMO_EMAIL_SANDBOX_PATH = "/demo/email-sandbox";
    public static final String DEMO_EMAIL_SANDBOX_SESSION_PATH = DEMO_EMAIL_SANDBOX_PATH + "/session";
    public static final String DEMO_EMAIL_SANDBOX_MESSAGES_PATH = DEMO_EMAIL_SANDBOX_PATH + "/messages";
}
