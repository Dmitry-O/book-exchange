package com.example.bookexchange.common.email;

import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.demoemail.DemoEmailSandboxService;
import com.example.bookexchange.common.util.UrlBuilder;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UrlBuilder urlBuilder;

    @Mock
    private AuditService auditService;

    @Mock
    private DemoEmailSandboxService demoEmailSandboxService;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setEmailSentFrom("noreply@book-exchange.test");

        emailService = new EmailServiceImpl(
                mailSender,
                templateEngine(),
                messageSource(),
                appProperties,
                urlBuilder,
                auditService,
                demoEmailSandboxService
        );
    }

    @Test
    void shouldRenderEnglishVerificationEmail() throws Exception {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        user.setLocale("en");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(urlBuilder.buildEmailActionUrl("verification-token", EmailType.CONFIRM_EMAIL))
                .thenReturn("http://localhost:5173/verify-email?token=verification-token");

        assertThatCode(() -> emailService.buildAndSendEmail(user, "verification-token", EmailType.CONFIRM_EMAIL))
                .doesNotThrowAnyException();

        assertThat(mimeMessage.getSubject()).isEqualTo("Verify your email address");
        assertThat(extractHtml(mimeMessage)).contains("Confirm your email to activate your account");
        assertThat(extractHtml(mimeMessage)).contains("http://localhost:5173/verify-email?token=verification-token");
        verify(demoEmailSandboxService).attachSandboxHeader("reader@example.com", mimeMessage);
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldRenderGermanResetPasswordEmail() throws Exception {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        user.setLocale("de");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(urlBuilder.buildEmailActionUrl("reset-token", EmailType.RESET_PASSWORD))
                .thenReturn("http://localhost:5173/reset-password?token=reset-token");

        assertThatCode(() -> emailService.buildAndSendEmail(user, "reset-token", EmailType.RESET_PASSWORD))
                .doesNotThrowAnyException();

        String html = extractHtml(mimeMessage);

        assertThat(mimeMessage.getSubject()).contains("Passwort");
        assertThat(html).contains("http://localhost:5173/reset-password?token=reset-token");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldRenderRussianDeleteAccountEmail() throws Exception {
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");
        user.setLocale("ru");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(urlBuilder.buildEmailActionUrl("delete-token", EmailType.DELETE_ACCOUNT))
                .thenReturn("http://localhost:5173/delete-account-confirm?token=delete-token");

        assertThatCode(() -> emailService.buildAndSendEmail(user, "delete-token", EmailType.DELETE_ACCOUNT))
                .doesNotThrowAnyException();

        String html = extractHtml(mimeMessage);

        assertThat(mimeMessage.getSubject()).isNotBlank();
        assertThat(html).contains("http://localhost:5173/delete-account-confirm?token=delete-token");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldRenderStructuredNotificationEmail() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationEmailRequest request = NotificationEmailRequest.builder()
                .emailTo("reader@example.com")
                .recipientName("reader_one")
                .locale("en")
                .subject("Exchange request updated")
                .preheader("Exchange request updated")
                .eyebrow("Exchange update")
                .title("Your exchange request was approved")
                .intro("The receiver approved your exchange request.")
                .summary("The exchange status is now APPROVED.")
                .highlights(List.of(
                        NotificationEmailBadge.builder()
                                .label("Status")
                                .value("Approved")
                                .tone("success")
                                .build(),
                        NotificationEmailBadge.builder()
                                .label("Changed at")
                                .value("2026-04-24T10:15:30Z")
                                .tone("neutral")
                                .build()
                ))
                .exchange(NotificationEmailExchange.builder()
                        .exchangeId("3101")
                        .referenceText("Exchange #3101")
                        .status(NotificationEmailBadge.builder()
                                .label("Status")
                                .value("Approved")
                                .tone("success")
                                .build())
                        .changedAt("24 Apr 2026, 10:15")
                        .leftBook(NotificationEmailBook.builder()
                                .title("Your book")
                                .name("Atomic Habits")
                                .subtitle("James Clear, 2018")
                                .meta("Self-help, Berlin")
                                .build())
                        .rightBook(NotificationEmailBook.builder()
                                .title("Requested book")
                                .name("The Hobbit")
                                .subtitle("J. R. R. Tolkien, 1937")
                                .meta("Fantasy, Munich")
                                .gift(true)
                                .build())
                        .leftUser(NotificationEmailUserCard.builder()
                                .title("You")
                                .name("reader_one")
                                .meta("reader@example.com")
                                .initial("R")
                                .build())
                        .rightUser(NotificationEmailUserCard.builder()
                                .title("Book owner")
                                .name("owner_two")
                                .meta("owner@example.com")
                                .initial("O")
                                .build())
                        .build())
                .details(List.of(
                        new NotificationEmailDetail("Exchange ID", "3101"),
                        new NotificationEmailDetail("Status", "APPROVED")
                ))
                .build();

        assertThatCode(() -> emailService.sendNotificationEmail(request))
                .doesNotThrowAnyException();

        String html = extractHtml(mimeMessage);

        assertThat(mimeMessage.getSubject()).isEqualTo("Exchange request updated");
        assertThat(html).contains("Your exchange request was approved");
        assertThat(html).contains("Exchange overview");
        assertThat(html).contains("24 Apr 2026, 10:15");
        assertThat(html).contains("Atomic Habits");
        assertThat(html).contains("The Hobbit");
        assertThat(html).contains("reader_one");
        assertThat(html).doesNotContain("Exchange ID");
        assertThat(html).doesNotContain("Exchange #3101");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldRenderStructuredReportNotificationSections() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationEmailRequest request = NotificationEmailRequest.builder()
                .emailTo("reader@example.com")
                .recipientName("reader_one")
                .locale("en")
                .subject("Your report was resolved")
                .preheader("Your report was resolved")
                .eyebrow("Report update")
                .title("Your report was resolved")
                .intro("An administrator resolved your report.")
                .summary("The report status is now RESOLVED.")
                .report(NotificationEmailReport.builder()
                        .title("Your moderation report")
                        .status(NotificationEmailBadge.builder()
                                .label("Status")
                                .value("Resolved")
                                .tone("success")
                                .build())
                        .targetText("Book #15")
                        .targetStateText("The reported book has been deleted. The card below shows the historical snapshot captured when you created the report.")
                        .targetModerationText("The reported user account is currently restricted by the admin team.")
                        .targetBook(NotificationEmailBook.builder()
                                .title("Reported book snapshot")
                                .name("Charley Smash")
                                .subtitle("Book #15")
                                .meta("Owner: owner_one")
                                .photoUrl("https://example.com/book.jpg")
                                .build())
                        .targetUser(NotificationEmailUserCard.builder()
                                .title("Reported user")
                                .name("owner_one")
                                .meta("User #42")
                                .photoUrl("https://example.com/user.jpg")
                                .initial("O")
                                .build())
                        .reason("Spam")
                        .comment("Looks suspicious")
                        .build())
                .details(List.of(
                        new NotificationEmailDetail("Report ID", "4101"),
                        new NotificationEmailDetail("Status", "RESOLVED")
                ))
                .build();

        assertThatCode(() -> emailService.sendNotificationEmail(request))
                .doesNotThrowAnyException();

        String html = extractHtml(mimeMessage);

        assertThat(mimeMessage.getSubject()).isEqualTo("Your report was resolved");
        assertThat(html).contains("Your moderation report");
        assertThat(html).contains("Reported book snapshot");
        assertThat(html).contains("Charley Smash");
        assertThat(html).contains("Owner: owner_one");
        assertThat(html).contains("Reported user");
        assertThat(html).contains("User #42");
        assertThat(html).contains("The reported book has been deleted. The card below shows the historical snapshot captured when you created the report.");
        assertThat(html).contains("The reported user account is currently restricted by the admin team.");
        assertThat(html).contains("Spam");
        assertThat(html).contains("Looks suspicious");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldRenderLocalizedNotificationStaticSectionsForGermanLocale() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationEmailRequest request = NotificationEmailRequest.builder()
                .emailTo("reader@example.com")
                .recipientName("reader_one")
                .locale("de")
                .subject("Ihre Beschwerde wurde gelöst")
                .preheader("Ihre Beschwerde wurde gelöst")
                .eyebrow("Beschwerde-Update")
                .title("Ihre Beschwerde wurde gelöst")
                .intro("Ein Admin hat Ihre Beschwerde gelöst.")
                .summary("Der Status der Beschwerde ist jetzt GELÖST.")
                .user(NotificationEmailUserCard.builder()
                        .title("Ihr Konto")
                        .name("reader_one")
                        .meta("reader@example.com")
                        .initial("R")
                        .build())
                .report(NotificationEmailReport.builder()
                        .title("Ihre Moderationsbeschwerde")
                        .status(NotificationEmailBadge.builder()
                                .label("Status")
                                .value("Gelöst")
                                .tone("success")
                                .build())
                        .targetText("Nutzer #42")
                        .reason("Spam")
                        .comment("Sieht verdächtig aus")
                        .build())
                .details(List.of(
                        new NotificationEmailDetail("Beschwerde-ID", "4101"),
                        new NotificationEmailDetail("Status", "GELÖST")
                ))
                .build();

        assertThatCode(() -> emailService.sendNotificationEmail(request))
                .doesNotThrowAnyException();

        String html = extractHtml(mimeMessage);

        assertThat(html).contains("Kontoübersicht");
        assertThat(html).contains("Spam");
        assertThat(html).contains("Grund:");
        assertThat(html).contains("Kommentar:");
        assertThat(html).doesNotContain("Details");
        verify(mailSender).send(mimeMessage);
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }

    private ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages_email");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    private String extractHtml(MimeMessage mimeMessage) throws Exception {
        String html = extractHtml(mimeMessage.getContent());

        if (html != null) {
            return html;
        }

        throw new IllegalStateException("Unsupported email content type: " + mimeMessage.getContent());
    }

    private String extractHtml(Object content) throws Exception {
        if (content instanceof String html) {
            return html;
        }

        if (content instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                String nested = extractHtml(multipart.getBodyPart(index).getContent());

                if (nested != null) {
                    return nested;
                }
            }
        }

        return null;
    }
}
