package com.example.bookexchange.common.demoemail;

import com.example.bookexchange.common.result.Result;
import jakarta.mail.internet.MimeMessage;

public interface DemoEmailSandboxService {

    String REQUEST_HEADER_NAME = "X-Demo-Email-Sandbox-Id";
    String MAIL_HEADER_NAME = "X-Book-Exchange-Demo-Sandbox";

    void attachSandboxHeader(String emailTo, MimeMessage message);

    Result<DemoEmailSandboxSessionDTO> createSession(String requestedSandboxId, String email);

    Result<DemoEmailInboxDTO> getInbox(String requestedSandboxId, Integer limit);

    Result<DemoEmailInboxDTO> clearInbox(String requestedSandboxId);
}
