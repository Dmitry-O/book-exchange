package com.example.bookexchange.common.email;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.util.UrlBuilder;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final AppProperties appProperties;
    private final UrlBuilder urlBuilder;
    private final AuditService auditService;

    public Result<Void> buildAndSendEmail(User user, String token, EmailType emailType) {
        String emailTo = user.getEmail();
        Locale locale = resolveLocale(user.getLocale());
        EmailContent content = buildEmailContent(user, token, emailType, locale);

        Context context = new Context(locale);
        context.setVariable("brandName", content.brandName());
        context.setVariable("preheader", content.preheader());
        context.setVariable("eyebrow", content.eyebrow());
        context.setVariable("title", content.title());
        context.setVariable("greeting", content.greeting());
        context.setVariable("intro", content.intro());
        context.setVariable("ctaLabel", content.ctaLabel());
        context.setVariable("actionUrl", content.actionUrl());
        context.setVariable("linkHint", content.linkHint());
        context.setVariable("expiryHint", content.expiryHint());
        context.setVariable("ignoreText", content.ignoreText());
        context.setVariable("signature", content.signature());

        String htmlTemplate = templateEngine.process("email/action_email", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailTo);
            helper.setSubject(content.subject());
            helper.setText(htmlTemplate, true);
            helper.setFrom(appProperties.getEmailSentFrom());

            mailSender.send(message);

            auditService.log(AuditEvent.builder()
                    .action("EMAIL_SENDING")
                    .result(AuditResult.SUCCESS)
                    .actorEmail(emailTo)
                    .detail("subject", content.subject())
                    .detail("locale", locale.toLanguageTag())
                    .build()
            );

            return ResultFactory.successVoid();
        } catch (Exception ex) {
            auditService.log(AuditEvent.builder()
                    .action("EMAIL_SENDING")
                    .result(AuditResult.ERROR)
                    .reason("SYSTEM_UNEXPECTED_ERROR")
                    .detail("emailTo", emailTo)
                    .detail("exception", ex.getMessage())
                    .build()
            );

            return ResultFactory.error(MessageKey.SYSTEM_UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private EmailContent buildEmailContent(User user, String token, EmailType emailType, Locale locale) {
        String brandName = message(locale, "email.common.brand");
        String actionUrl = urlBuilder.buildEmailActionUrl(token, emailType);
        String displayName = user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname()
                : user.getEmail();

        String subject = switch (emailType) {
            case CONFIRM_EMAIL -> message(locale, "email.verify.subject");
            case RESET_PASSWORD -> message(locale, "email.reset.subject");
            case DELETE_ACCOUNT -> message(locale, "email.delete.subject");
        };

        String eyebrow = switch (emailType) {
            case CONFIRM_EMAIL -> message(locale, "email.verify.eyebrow");
            case RESET_PASSWORD -> message(locale, "email.reset.eyebrow");
            case DELETE_ACCOUNT -> message(locale, "email.delete.eyebrow");
        };

        String title = switch (emailType) {
            case CONFIRM_EMAIL -> message(locale, "email.verify.title");
            case RESET_PASSWORD -> message(locale, "email.reset.title");
            case DELETE_ACCOUNT -> message(locale, "email.delete.title");
        };

        String intro = switch (emailType) {
            case CONFIRM_EMAIL -> message(locale, "email.verify.intro");
            case RESET_PASSWORD -> message(locale, "email.reset.intro");
            case DELETE_ACCOUNT -> message(locale, "email.delete.intro");
        };

        String ctaLabel = switch (emailType) {
            case CONFIRM_EMAIL -> message(locale, "email.verify.cta");
            case RESET_PASSWORD -> message(locale, "email.reset.cta");
            case DELETE_ACCOUNT -> message(locale, "email.delete.cta");
        };

        String ignoreText = switch (emailType) {
            case CONFIRM_EMAIL -> message(locale, "email.verify.ignore");
            case RESET_PASSWORD -> message(locale, "email.reset.ignore");
            case DELETE_ACCOUNT -> message(locale, "email.delete.ignore");
        };

        return new EmailContent(
                brandName,
                subject,
                title,
                title,
                eyebrow,
                message(locale, "email.common.greeting", displayName),
                intro,
                ctaLabel,
                actionUrl,
                message(locale, "email.common.linkHint"),
                message(locale, "email.common.expiry"),
                ignoreText,
                message(locale, "email.common.signature", brandName)
        );
    }

    private Locale resolveLocale(String localeValue) {
        if (localeValue == null || localeValue.isBlank()) {
            return Locale.ENGLISH;
        }

        Locale locale = Locale.forLanguageTag(localeValue);

        return locale.getLanguage() == null || locale.getLanguage().isBlank()
                ? Locale.ENGLISH
                : locale;
    }

    private String message(Locale locale, String key, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    private record EmailContent(
            String brandName,
            String subject,
            String preheader,
            String title,
            String eyebrow,
            String greeting,
            String intro,
            String ctaLabel,
            String actionUrl,
            String linkHint,
            String expiryHint,
            String ignoreText,
            String signature
    ) { }
}
