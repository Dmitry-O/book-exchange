package com.example.bookexchange.repositories;

import com.example.bookexchange.models.TokenType;
import com.example.bookexchange.models.User;
import com.example.bookexchange.models.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUserAndType(User user, TokenType type);
}
