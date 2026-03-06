ALTER TABLE app_user
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE verification_token (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,

     token VARCHAR(255) NOT NULL UNIQUE,

     user_id BIGINT NOT NULL,

     type VARCHAR(50) NOT NULL,

     expiry_date TIMESTAMP NOT NULL,

     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_verification_token_user
         FOREIGN KEY (user_id)
             REFERENCES app_user(id)
             ON DELETE CASCADE
);

CREATE INDEX idx_verification_token_token
    ON verification_token(token);

CREATE INDEX idx_verification_token_user
    ON verification_token(user_id);