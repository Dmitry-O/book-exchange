ALTER TABLE exchange
    ADD COLUMN update_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE exchange
SET update_created_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP);

CREATE INDEX idx_exchange_update_created_at ON exchange(update_created_at);
