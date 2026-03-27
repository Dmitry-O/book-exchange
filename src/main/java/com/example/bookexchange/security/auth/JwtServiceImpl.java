package com.example.bookexchange.security.auth;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.user.model.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final String jwtSecretKey;
    private final int accessTokenTTL;

    public JwtServiceImpl(AppProperties appProperties) {
        jwtSecretKey = appProperties.getJwtSecretKey();
        accessTokenTTL = appProperties.getAccessTokenTimeToLive();
    }

    @Override
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(accessTokenTTL, ChronoUnit.SECONDS)))
                .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes()))
                .compact();
    }

    @Override
    public Long extractUserId(String token) {
        String subject = Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException ex) {
            throw new JwtException("Invalid JWT subject", ex);
        }
    }
}
