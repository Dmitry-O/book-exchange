CREATE UNIQUE INDEX uk_user_email_deleted ON app_user(email, deleted_at);
CREATE UNIQUE INDEX uk_user_nickname_deleted ON app_user(nickname, deleted_at);

CREATE INDEX idx_app_user_deleted_created ON app_user(deleted_at, created_at);
CREATE INDEX idx_book_deleted_created ON book(deleted_at, created_at);
CREATE INDEX idx_exchange_deleted_created ON exchange(deleted_at, created_at);
CREATE INDEX idx_report_deleted_created ON report(deleted_at, created_at);
