package com.example.bookexchange.services;

import com.example.bookexchange.config.AppProperties;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.models.EmailType;
import com.example.bookexchange.models.MessageKey;
import com.example.bookexchange.util.UrlBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final AppProperties appProperties;

    @Autowired
    private UrlBuilder urlBuilder;

    public void sendEmail(String emailTo, String subject, String htmlTemplate) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailTo);
            helper.setSubject(subject);
            helper.setText(htmlTemplate, true);
            helper.setFrom(appProperties.getEmailSentFrom());

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public void buildAndSendEmail(String emailTo, String token, EmailType emailType) {
        String url = urlBuilder.buildEmailVerificationUrl(token, emailType), subject, template = "email/", templateVariableName;

        switch (emailType) {
            case EmailType.CONFIRM_EMAIL -> {
                template += "verification_email";
                subject = "Verify your email";
                templateVariableName = "verificationUrl";
            }
            case EmailType.RESET_PASSWORD -> {
                template += "reset_password_email";
                subject = "Reset your password";
                templateVariableName = "resetPasswordUrl";
            }
            case EmailType.DELETE_ACCOUNT -> {
                template += "delete_account_email";
                subject = "Verify deleting your account";
                templateVariableName = "deleteAccountUrl";
            }
            default -> throw new BadRequestException(MessageKey.SYSTEM_WRONG_EMAIL_TYPE);
        }

        Context context = new Context();
        context.setVariable(templateVariableName, url);

        String htmlTemplate = templateEngine.process(
                template,
                context
        );

        sendEmail(emailTo, subject, htmlTemplate);
    }
}
