ALTER TABLE book
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL,
    ADD COLUMN created_request_id BINARY(16) NULL,
    ADD COLUMN updated_request_id BINARY(16) NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN deleted_at TIMESTAMP NULL;

ALTER TABLE book
    ADD CONSTRAINT fk_book_created_by
        FOREIGN KEY (created_by) REFERENCES app_user(id);

ALTER TABLE book
    ADD CONSTRAINT fk_book_updated_by
        FOREIGN KEY (updated_by) REFERENCES app_user(id);

CREATE INDEX idx_book_created_at ON book(created_at);
CREATE INDEX idx_book_deleted_at ON book(deleted_at);
CREATE INDEX idx_book_created_by ON book(created_by);
CREATE INDEX idx_book_updated_by ON book(updated_by);
CREATE INDEX idx_book_created_request_id ON book(created_request_id);