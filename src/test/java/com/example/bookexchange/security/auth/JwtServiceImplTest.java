package com.example.bookexchange.security.auth;

import com.example.bookexchange.common.config.AppProperties;
import com.example.bookexchange.support.unit.UnitFixtureIds;
import com.example.bookexchange.support.unit.UnitTestDataFactory;
import com.example.bookexchange.user.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceImplTest {

    @Test
    void shouldCreateReadableToken_whenGenerateTokenIsCalled() {
        AppProperties appProperties = new AppProperties();
        appProperties.setJwtSecretKey("01234567890123456789012345678901");
        appProperties.setAccessTokenTimeToLive(3600);
        JwtServiceImpl jwtService = new JwtServiceImpl(appProperties);
        User user = UnitTestDataFactory.user(UnitFixtureIds.VERIFIED_USER_ID, "reader@example.com", "reader_one");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    void shouldThrow_whenExtractUserIdSubjectIsNotNumeric() {
        String secret = "01234567890123456789012345678901";
        AppProperties appProperties = new AppProperties();
        appProperties.setJwtSecretKey(secret);
        appProperties.setAccessTokenTimeToLive(3600);
        JwtServiceImpl jwtService = new JwtServiceImpl(appProperties);

        String token = Jwts.builder()
                .setSubject("not-a-number")
                .setIssuedAt(new Date())
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();

        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(io.jsonwebtoken.JwtException.class)
                .hasMessage("Invalid JWT subject");
    }
}
