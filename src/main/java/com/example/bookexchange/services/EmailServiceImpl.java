package com.example.bookexchange.services;

import com.example.bookexchange.config.AppProperties;
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

    public void sendVerificationEmail(String emailTo, String token) {
        String verificationUrl = urlBuilder.buildEmailConfirmationUrl(token);

        Context context = new Context();
        context.setVariable("verificationUrl", verificationUrl);

        String htmlTemplate = templateEngine.process(
                "email/verification_email",
                context
        );

        sendEmail(emailTo, "Verify your email", htmlTemplate);
    }

    public void sendResetPasswordEmail(String emailTo, String token) {
        String resetPasswordUrl = urlBuilder.buildResetPasswordUrl(token);

        Context context = new Context();
        context.setVariable("resetPasswordUrl", resetPasswordUrl);

        String htmlTemplate = templateEngine.process(
                "email/reset_password_email",
                context
        );

        sendEmail(emailTo, "Reset your password", htmlTemplate);
    }
}
