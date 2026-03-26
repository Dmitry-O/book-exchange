package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.model.TokenType;
import com.example.bookexchange.auth.model.VerificationToken;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.user.model.User;

public interface VerificationTokenService {

    Result<String> createToken(User user, TokenType tokenType);

    Result<VerificationToken> validateToken(String token, TokenType expectedType, String action);

    Result<User> deleteUserTokens(User user);

    void deleteToken(VerificationToken verificationToken);

    void deleteByUserAndType(User user, TokenType tokenType);
}
