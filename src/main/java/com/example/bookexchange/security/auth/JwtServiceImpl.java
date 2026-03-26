package com.example.bookexchange.security.auth;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final String jwtSecretKey;
    private final UserRepository userRepository;

    public JwtServiceImpl(AppProperties appProperties, UserRepository userRepository) {
        jwtSecretKey = appProperties.getJwtSecretKey();
        this.userRepository = userRepository;
    }

    @Override
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(3, ChronoUnit.HOURS))) // TODO: to be decreased before release
                .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes()))
                .compact();
    }

    @Override
    public Long extractUserId(String token) {
         Long userId = Long.parseLong(
                 Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject()
         );

        User user = userRepository.findById(userId).orElseThrow();

        if (user.getDeletedAt() != null) {
            throw new RuntimeException();
        }

        return userId;
    }
}
