ALTER TABLE exchange
    DROP INDEX idx_exchange_deleted_created,
    DROP COLUMN deleted_at;

ALTER TABLE report
    DROP INDEX idx_report_deleted_at,
    DROP INDEX idx_report_deleted_created,
    DROP COLUMN deleted_at;

CREATE INDEX idx_book_user_exchanged ON book(user_id, is_exchanged);
CREATE INDEX idx_book_user_exchanged_created ON book(user_id, is_exchanged, created_at DESC);
CREATE INDEX idx_book_category ON book(category);
CREATE INDEX idx_book_city ON book(city);
CREATE INDEX idx_book_search ON book(category, city, is_exchanged);
CREATE INDEX idx_book_not_deleted ON book(deleted_at);

CREATE UNIQUE INDEX idx_user_email_unique ON app_user(email);
CREATE UNIQUE INDEX idx_user_nickname_unique ON app_user(nickname);
CREATE INDEX idx_user_email_verified ON app_user(email_verified);
CREATE INDEX idx_user_not_deleted ON app_user(deleted_at);
CREATE INDEX idx_user_banned_until ON app_user(banned_until);