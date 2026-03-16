ALTER TABLE app_user
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT,
    ADD COLUMN updated_by BIGINT,
    ADD COLUMN created_request_id BINARY(16),
    ADD COLUMN updated_request_id BINARY(16),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX idx_app_user_created_at ON app_user(created_at);
CREATE INDEX idx_app_user_deleted_at ON app_user(deleted_at);
CREATE INDEX idx_app_user_created_by ON app_user(created_by);
CREATE INDEX idx_app_user_updated_by ON app_user(updated_by);
CREATE INDEX idx_app_user_created_request_id ON app_user(created_request_id);