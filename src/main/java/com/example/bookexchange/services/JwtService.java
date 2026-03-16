package com.example.bookexchange.services;

import com.example.bookexchange.models.User;
import org.springframework.stereotype.Service;

@Service
public interface JwtService {

    String generateToken(User user);

    Long extractUserId(String token);
}
