package com.example.bookexchange.common.email;

import com.example.bookexchange.user.model.User;
import com.example.bookexchange.common.result.Result;

public interface EmailService {

    Result<Void> buildAndSendEmail(User user, String token, EmailType emailType);
}
