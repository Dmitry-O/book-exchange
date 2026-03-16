ALTER TABLE report
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN updated_by BIGINT NULL,
    ADD COLUMN created_request_id BINARY(16) NULL,
    ADD COLUMN updated_request_id BINARY(16) NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN deleted_at TIMESTAMP NULL;

ALTER TABLE report
    ADD CONSTRAINT fk_report_created_by
        FOREIGN KEY (created_by) REFERENCES app_user(id);

ALTER TABLE report
    ADD CONSTRAINT fk_report_updated_by
        FOREIGN KEY (updated_by) REFERENCES app_user(id);

CREATE INDEX idx_report_created_at ON report(created_at);
CREATE INDEX idx_report_deleted_at ON report(deleted_at);
CREATE INDEX idx_report_created_by ON report(created_by);
CREATE INDEX idx_report_updated_by ON report(updated_by);
CREATE INDEX idx_report_created_request_id ON report(created_request_id);