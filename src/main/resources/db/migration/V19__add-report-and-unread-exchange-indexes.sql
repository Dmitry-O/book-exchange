CREATE INDEX idx_report_reporter_created
    ON report(reporter_id, created_at DESC);

CREATE INDEX idx_exchange_sender_read_updated
    ON exchange(sender_user_id, is_read_by_sender, updated_at DESC);

CREATE INDEX idx_exchange_receiver_read_updated
    ON exchange(receiver_user_id, is_read_by_receiver, updated_at DESC);
