package com.example.bookexchange.common.demoemail;

import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.demo-email-sandbox.enabled", havingValue = "false", matchIfMissing = true)
public class NoopDemoEmailSandboxService implements DemoEmailSandboxService {

    @Override
    public void attachSandboxHeader(String emailTo, MimeMessage message) {
        // Demo email sandbox is disabled outside the demo profile.
    }

    @Override
    public Result<DemoEmailSandboxSessionDTO> createSession(String requestedSandboxId, String email) {
        return ResultFactory.ok(new DemoEmailSandboxSessionDTO(null, null, false));
    }

    @Override
    public Result<DemoEmailInboxDTO> getInbox(String requestedSandboxId, Integer limit) {
        return ResultFactory.ok(new DemoEmailInboxDTO(null, null, List.of()));
    }

    @Override
    public Result<DemoEmailInboxDTO> clearInbox(String requestedSandboxId) {
        return ResultFactory.ok(new DemoEmailInboxDTO(null, null, List.of()));
    }
}
