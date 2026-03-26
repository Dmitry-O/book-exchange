package com.example.bookexchange.common.email;

import com.example.bookexchange.common.result.Result;

public interface EmailService {

    Result<Void> buildAndSendEmail(String emailTo, String token, EmailType emailType);
}
