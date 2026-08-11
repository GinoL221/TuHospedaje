ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_delivery_warning_pending BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS email_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    email_type VARCHAR(32) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    recipient VARCHAR(256) NOT NULL,
    subject VARCHAR(256) NOT NULL,
    html_body TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    lease_token VARCHAR(36) NULL,
    lease_until DATETIME(6) NULL,
    error_code VARCHAR(64) NULL,
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_email_outbox_type_aggregate UNIQUE (email_type, aggregate_id),
    INDEX IX_email_outbox_status_lease_until (status, lease_until),
    INDEX IX_email_outbox_completed_at (completed_at),
    CONSTRAINT FK_email_outbox_user FOREIGN KEY (user_id) REFERENCES users (id)
);
