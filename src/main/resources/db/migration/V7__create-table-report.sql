CREATE TABLE report (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     target_type VARCHAR(20) NOT NULL,
     target_id BIGINT NOT NULL,
     reason VARCHAR(50) NOT NULL,
     comment TEXT,
     status VARCHAR(20) NOT NULL,
     reporter_id BIGINT NOT NULL,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_report_reporter
         FOREIGN KEY (reporter_id)
             REFERENCES app_user(id)
             ON DELETE CASCADE
);

ALTER TABLE report
    ADD CONSTRAINT unique_user_report
        UNIQUE (reporter_id, target_type, target_id);

CREATE INDEX idx_report_target
    ON report(target_type, target_id);

CREATE INDEX idx_report_status
    ON report(status);