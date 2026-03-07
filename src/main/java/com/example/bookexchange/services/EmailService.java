package com.example.bookexchange.services;

public interface EmailService {

    void sendEmail(String emailTo, String subject, String htmlTemplate);

    void sendVerificationEmail(String emailTo, String token);

    void sendResetPasswordEmail(String emailTo, String token);
}
