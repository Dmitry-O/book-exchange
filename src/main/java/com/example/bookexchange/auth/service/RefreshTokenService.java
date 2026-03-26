package com.example.bookexchange.auth.service;

import com.example.bookexchange.auth.model.RefreshToken;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.user.model.User;

public interface RefreshTokenService {

    String createToken(User user);

    Result<RefreshToken> validateToken(String token);

    Result<Void> deleteToken(Long userId, String token);

    Result<User> deleteUserTokens(User user);
}
