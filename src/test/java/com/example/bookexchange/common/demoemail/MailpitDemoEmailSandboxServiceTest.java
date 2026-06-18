package com.example.bookexchange.common.demoemail;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.result.Success;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(MockitoExtension.class)
class MailpitDemoEmailSandboxServiceTest {

    private static final String SANDBOX_ID = "sandbox_12345678901234567890123456789012";
    private static final String OTHER_SANDBOX_ID = "sandbox_abcdefghijklmnopqrstuvwxyz987654";

    @Mock
    private MailpitClient mailpitClient;

    private MailpitDemoEmailSandboxService demoEmailSandboxService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getDemoEmailSandbox().setEnabled(true);
        appProperties.getDemoEmailSandbox().setSessionTtlMinutes(180);
        appProperties.getDemoEmailSandbox().setMaxMessages(10);

        demoEmailSandboxService = new MailpitDemoEmailSandboxService(appProperties, mailpitClient);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldAttachSandboxHeaderFromEmailSession() throws Exception {
        demoEmailSandboxService.createSession(SANDBOX_ID, "reader@example.com");
        MockHttpServletResponse response = bindRequestWithSandboxId(SANDBOX_ID);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

        demoEmailSandboxService.attachSandboxHeader("reader@example.com", message);

        assertThat(message.getHeader(DemoEmailSandboxService.MAIL_HEADER_NAME)).containsExactly(SANDBOX_ID);
        assertThat(response.getHeader(DemoEmailSandboxService.REQUEST_HEADER_NAME)).isEqualTo(SANDBOX_ID);
    }

    @Test
    void shouldAttachSandboxHeaderWithoutRequest_whenRecipientWasPreviouslyBoundToSession() throws Exception {
        demoEmailSandboxService.createSession(SANDBOX_ID, "reader@example.com");
        RequestContextHolder.resetRequestAttributes();

        MimeMessage asyncMessage = new MimeMessage(Session.getInstance(new Properties()));
        demoEmailSandboxService.attachSandboxHeader("reader@example.com", asyncMessage);

        assertThat(asyncMessage.getHeader(DemoEmailSandboxService.MAIL_HEADER_NAME)).containsExactly(SANDBOX_ID);
    }

    @Test
    void shouldBindEmailToSession_whenCreatingSessionForEmail() throws Exception {
        demoEmailSandboxService.createSession(SANDBOX_ID, "Reader@Example.com");

        MimeMessage asyncMessage = new MimeMessage(Session.getInstance(new Properties()));
        demoEmailSandboxService.attachSandboxHeader("reader@example.com", asyncMessage);

        assertThat(asyncMessage.getHeader(DemoEmailSandboxService.MAIL_HEADER_NAME)).containsExactly(SANDBOX_ID);
    }

    @Test
    void shouldKeepSeparateInboxSessions_whenBrowserSwitchesBetweenEmails() {
        Success<DemoEmailSandboxSessionDTO> readerSession = assertSuccess(
                demoEmailSandboxService.createSession(SANDBOX_ID, "reader@example.com"),
                OK
        );
        Success<DemoEmailSandboxSessionDTO> ownerSession = assertSuccess(
                demoEmailSandboxService.createSession(SANDBOX_ID, "owner@example.com"),
                OK
        );
        Success<DemoEmailSandboxSessionDTO> readerSessionAgain = assertSuccess(
                demoEmailSandboxService.createSession(ownerSession.body().sandboxId(), "reader@example.com"),
                OK
        );

        assertThat(readerSession.body().sandboxId()).isEqualTo(SANDBOX_ID);
        assertThat(ownerSession.body().sandboxId()).isNotEqualTo(SANDBOX_ID);
        assertThat(ownerSession.body().sandboxId()).matches("[A-Za-z0-9_-]{32,128}");
        assertThat(readerSessionAgain.body().sandboxId()).isEqualTo(SANDBOX_ID);
    }

    @Test
    void shouldAttachRecipientOwnedSandbox_whenCurrentRequestBelongsToAnotherEmail() throws Exception {
        demoEmailSandboxService.createSession(SANDBOX_ID, "admin@example.com");
        MockHttpServletResponse response = bindRequestWithSandboxId(SANDBOX_ID);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

        demoEmailSandboxService.attachSandboxHeader("reader@example.com", message);

        String recipientSandboxId = message.getHeader(DemoEmailSandboxService.MAIL_HEADER_NAME)[0];
        assertThat(response.getHeader(DemoEmailSandboxService.REQUEST_HEADER_NAME)).isNull();

        Success<DemoEmailSandboxSessionDTO> readerSession = assertSuccess(
                demoEmailSandboxService.createSession(null, "reader@example.com"),
                OK
        );

        assertThat(recipientSandboxId).isNotEqualTo(SANDBOX_ID);
        assertThat(recipientSandboxId).matches("[A-Za-z0-9_-]{32,128}");
        assertThat(readerSession.body().sandboxId()).isEqualTo(recipientSandboxId);
    }

    @Test
    void shouldNotOverrideExistingEmailBindingWithAnotherRequestSandbox() throws Exception {
        demoEmailSandboxService.createSession(SANDBOX_ID, "reader@example.com");
        MockHttpServletResponse response = bindRequestWithSandboxId(OTHER_SANDBOX_ID);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

        demoEmailSandboxService.attachSandboxHeader("reader@example.com", message);

        assertThat(message.getHeader(DemoEmailSandboxService.MAIL_HEADER_NAME)).containsExactly(SANDBOX_ID);
        assertThat(response.getHeader(DemoEmailSandboxService.REQUEST_HEADER_NAME)).isNull();
    }

    @Test
    void shouldCreateRecipientOwnedSession_whenNoRequestHeaderAndEmailIsUnknown() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

        demoEmailSandboxService.attachSandboxHeader("reader@example.com", message);

        String recipientSandboxId = message.getHeader(DemoEmailSandboxService.MAIL_HEADER_NAME)[0];
        Success<DemoEmailSandboxSessionDTO> readerSession = assertSuccess(
                demoEmailSandboxService.createSession(null, "reader@example.com"),
                OK
        );

        assertThat(recipientSandboxId).matches("[A-Za-z0-9_-]{32,128}");
        assertThat(readerSession.body().sandboxId()).isEqualTo(recipientSandboxId);
    }

    @Test
    void shouldReturnOnlyMessagesMatchingSandboxHeader() {
        Instant now = Instant.parse("2026-06-04T12:00:00Z");
        when(mailpitClient.listMessages(10)).thenReturn(List.of(
                new MailpitMessageSummary("message-1", "noreply@test.com", List.of("reader@example.com"), "Verify", "Verify account", now),
                new MailpitMessageSummary("message-2", "noreply@test.com", List.of("other@example.com"), "Reset", "Reset password", now)
        ));
        when(mailpitClient.getHeaders("message-1")).thenReturn(Map.of(DemoEmailSandboxService.MAIL_HEADER_NAME, List.of(SANDBOX_ID)));
        when(mailpitClient.getHeaders("message-2")).thenReturn(Map.of(DemoEmailSandboxService.MAIL_HEADER_NAME, List.of("another-sandbox")));
        when(mailpitClient.getMessage("message-1")).thenReturn(new MailpitMessageDetail(
                "message-1",
                "noreply@test.com",
                List.of("reader@example.com"),
                "Verify",
                "Verify account",
                now,
                "<a href=\"https://example.com/verify\">verify</a>",
                "verify"
        ));

        Success<DemoEmailInboxDTO> success = assertSuccess(demoEmailSandboxService.getInbox(SANDBOX_ID, 10), OK);

        assertThat(success.body().sandboxId()).isEqualTo(SANDBOX_ID);
        assertThat(success.body().messages()).hasSize(1);
        assertThat(success.body().messages().getFirst().id()).isEqualTo("message-1");
        assertThat(success.body().messages().getFirst().html()).contains("verify");
    }

    @Test
    void shouldNotExtendSessionExpiration_whenReadingInbox() {
        Success<DemoEmailSandboxSessionDTO> session = assertSuccess(
                demoEmailSandboxService.createSession(SANDBOX_ID, "reader@example.com"),
                OK
        );
        when(mailpitClient.listMessages(10)).thenReturn(List.of());

        Success<DemoEmailInboxDTO> inbox = assertSuccess(
                demoEmailSandboxService.getInbox(SANDBOX_ID, 10),
                OK
        );

        assertThat(inbox.body().sandboxId()).isEqualTo(SANDBOX_ID);
        assertThat(inbox.body().expiresAt()).isEqualTo(session.body().expiresAt());
    }

    @Test
    void shouldDeleteOnlyMessagesMatchingSandboxHeader_whenClearingInbox() {
        Instant now = Instant.parse("2026-06-04T12:00:00Z");
        when(mailpitClient.listMessages(10)).thenReturn(List.of(
                new MailpitMessageSummary("message-1", "noreply@test.com", List.of("reader@example.com"), "Verify", "Verify account", now),
                new MailpitMessageSummary("message-2", "noreply@test.com", List.of("other@example.com"), "Reset", "Reset password", now)
        ));
        when(mailpitClient.getHeaders("message-1")).thenReturn(Map.of(DemoEmailSandboxService.MAIL_HEADER_NAME, List.of(SANDBOX_ID)));
        when(mailpitClient.getHeaders("message-2")).thenReturn(Map.of(DemoEmailSandboxService.MAIL_HEADER_NAME, List.of("another-sandbox")));

        Success<DemoEmailInboxDTO> success = assertSuccess(demoEmailSandboxService.clearInbox(SANDBOX_ID), OK);

        assertThat(success.body().messages()).isEmpty();
        verify(mailpitClient).deleteMessages(List.of("message-1"));
    }

    private MockHttpServletResponse bindRequestWithSandboxId(String sandboxId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DemoEmailSandboxService.REQUEST_HEADER_NAME, sandboxId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        return response;
    }
}
