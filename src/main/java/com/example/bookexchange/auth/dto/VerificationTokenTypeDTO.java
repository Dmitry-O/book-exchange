package com.example.bookexchange.auth.dto;

import com.example.bookexchange.auth.model.TokenType;

public enum VerificationTokenTypeDTO {
    CONFIRM_EMAIL(TokenType.CONFIRM_EMAIL),
    RESET_PASSWORD(TokenType.RESET_PASSWORD),
    DELETE_ACCOUNT(TokenType.DELETE_ACCOUNT);

    private final TokenType tokenType;

    VerificationTokenTypeDTO(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public TokenType toTokenType() {
        return tokenType;
    }
}
