ALTER TABLE exchange
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL,
    ADD COLUMN created_request_id BINARY(16) NULL,
    ADD COLUMN updated_request_id BINARY(16) NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN deleted_at TIMESTAMP NULL;

ALTER TABLE exchange
    ADD CONSTRAINT fk_exchange_created_by
        FOREIGN KEY (created_by) REFERENCES app_user(id);

ALTER TABLE exchange
    ADD CONSTRAINT fk_exchange_updated_by
        FOREIGN KEY (updated_by) REFERENCES app_user(id);

CREATE INDEX idx_exchange_created_at ON exchange(created_at);
CREATE INDEX idx_exchange_deleted_at ON exchange(deleted_at);
CREATE INDEX idx_exchange_created_by ON exchange(created_by);
CREATE INDEX idx_exchange_updated_by ON exchange(updated_by);
CREATE INDEX idx_exchange_created_request_id ON exchange(created_request_id);