package com.example.bookexchange.repositories;

import com.example.bookexchange.models.TokenType;
import com.example.bookexchange.models.User;
import com.example.bookexchange.models.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUserAndType(User user, TokenType type);

    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") Instant now);
}
