CREATE INDEX idx_exchange_sender_status_updated
    ON exchange(sender_user_id, status, updated_at);

CREATE INDEX idx_exchange_receiver_status_updated
    ON exchange(receiver_user_id, status, updated_at);

CREATE INDEX idx_exchange_sender_book_status
    ON exchange(sender_book_id, status);

CREATE INDEX idx_exchange_receiver_book_status
    ON exchange(receiver_book_id, status);

CREATE INDEX idx_exchange_status_updated
    ON exchange(status, updated_at);

CREATE INDEX idx_report_status_created
    ON report(status, created_at);
