package com.example.bookexchange.common.email;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.common.audit.model.AuditEvent;
import com.example.bookexchange.common.audit.model.AuditResult;
import com.example.bookexchange.common.audit.service.AuditService;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.util.UrlBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final UrlBuilder urlBuilder;
    private final AuditService auditService;

    public Result<Void> buildAndSendEmail(String emailTo, String token, EmailType emailType) {
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
            default -> {
                auditService.log(AuditEvent.builder()
                        .action("EMAIL_SENDING")
                        .result(AuditResult.FAILURE)
                        .actorEmail(emailTo)
                        .reason("SYSTEM_WRONG_EMAIL_TYPE")
                        .build()
                );

                return ResultFactory.error(MessageKey.SYSTEM_WRONG_EMAIL_TYPE, HttpStatus.BAD_REQUEST);
            }
        }

        Context context = new Context();
        context.setVariable(templateVariableName, url);

        String htmlTemplate = templateEngine.process(
                template,
                context
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailTo);
            helper.setSubject(subject);
            helper.setText(htmlTemplate, true);
            helper.setFrom(appProperties.getEmailSentFrom());

            mailSender.send(message);

            auditService.log(AuditEvent.builder()
                    .action("EMAIL_SENDING")
                    .result(AuditResult.SUCCESS)
                    .actorEmail(emailTo)
                    .detail("subject", subject)
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
}
