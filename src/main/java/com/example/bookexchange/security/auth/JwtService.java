package com.example.bookexchange.security.auth;

import com.example.bookexchange.user.model.User;
import org.springframework.stereotype.Service;

@Service
public interface JwtService {

    String generateToken(User user);

    Long extractUserId(String token);
}
