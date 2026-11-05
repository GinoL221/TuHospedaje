-- Revoke previously-issued access tokens on password change via token_version.
ALTER TABLE users ADD COLUMN password_changed_at DATETIME(6) NULL;
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 1;
