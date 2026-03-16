package com.example.bookexchange.services;

import com.example.bookexchange.models.EmailType;

public interface EmailService {

    void sendEmail(String emailTo, String subject, String htmlTemplate);

    void buildAndSendEmail(String emailTo, String token, EmailType emailType);
}
