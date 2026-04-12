package com.example.bookexchange.common.email;

import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.config.AppProperties;
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
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;

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
                auditService
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

        assertThat(mimeMessage.getSubject()).isEqualTo("Setzen Sie Ihr Passwort zurück");
        assertThat(extractHtml(mimeMessage)).contains("Setzen Sie Ihr Passwort sicher zurück");
        assertThat(extractHtml(mimeMessage)).contains("http://localhost:5173/reset-password?token=reset-token");
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

        assertThat(mimeMessage.getSubject()).isEqualTo("Подтвердите удаление аккаунта");
        assertThat(extractHtml(mimeMessage)).contains("Удалите аккаунт безопасно");
        assertThat(extractHtml(mimeMessage)).contains("http://localhost:5173/delete-account-confirm?token=delete-token");
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
