package com.example.bookexchange.services;

import com.example.bookexchange.core.result.Result;
import com.example.bookexchange.models.EmailType;

public interface EmailService {

    void sendEmail(String emailTo, String subject, String htmlTemplate);

    Result<Void> buildAndSendEmail(String emailTo, String token, EmailType emailType);
}
