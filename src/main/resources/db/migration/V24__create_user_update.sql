CREATE TABLE user_update (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BIT NOT NULL DEFAULT 0,
    target_url VARCHAR(255),
    book_id BIGINT,
    book_name VARCHAR(25),
    book_photo_url VARCHAR(1024),
    book_gift BIT,
    target_user_id BIGINT,
    target_user_nickname VARCHAR(20),
    target_user_photo_url VARCHAR(1024),
    report_id BIGINT,
    report_target_type VARCHAR(20),
    report_reason VARCHAR(50),
    report_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_request_id VARCHAR(255),
    updated_request_id VARCHAR(255),

    CONSTRAINT fk_user_update_user
        FOREIGN KEY (user_id)
            REFERENCES app_user(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_user_update_user_created
    ON user_update(user_id, created_at, id);

CREATE INDEX idx_user_update_user_read_created
    ON user_update(user_id, is_read, created_at, id);
