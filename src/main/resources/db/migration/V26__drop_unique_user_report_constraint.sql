ALTER TABLE report
    DROP INDEX unique_user_report;

CREATE INDEX idx_report_reporter_target_status
    ON report(reporter_id, target_type, target_id, status);
