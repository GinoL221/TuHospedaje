ALTER TABLE email_outbox
    ADD COLUMN IF NOT EXISTS next_attempt_at DATETIME(6) NULL AFTER failed_attempts;

CREATE INDEX IF NOT EXISTS IX_email_outbox_status_next_attempt
    ON email_outbox (status, next_attempt_at);
