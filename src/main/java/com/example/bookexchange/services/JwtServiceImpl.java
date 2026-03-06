package com.example.bookexchange.services;

import com.example.bookexchange.config.AppProperties;
import com.example.bookexchange.models.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final String jwtSecretKey;

    public JwtServiceImpl(AppProperties appProperties) {
        jwtSecretKey = appProperties.getJwtSecretKey();
    }

    @Override
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(3, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes()))
                .compact();
    }

    @Override
    public String extractUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
