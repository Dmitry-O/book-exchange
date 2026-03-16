ALTER TABLE app_user
    MODIFY COLUMN created_request_id CHAR(36),
    MODIFY COLUMN updated_request_id CHAR(36);

ALTER TABLE book
    MODIFY COLUMN created_request_id CHAR(36),
    MODIFY COLUMN updated_request_id CHAR(36);

ALTER TABLE exchange
    MODIFY COLUMN created_request_id CHAR(36),
    MODIFY COLUMN updated_request_id CHAR(36);

ALTER TABLE report
    MODIFY COLUMN created_request_id CHAR(36),
    MODIFY COLUMN updated_request_id CHAR(36);