ALTER TABLE users
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS refresh_token_families (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_uuid BINARY(16) NOT NULL,
    user_id BIGINT NOT NULL,
    current_generation BIGINT NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    absolute_expires_at DATETIME(6) NOT NULL,
    last_rotated_at DATETIME(6) NULL,
    last_seen_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    revocation_reason VARCHAR(32) NULL,
    reuse_detected_at DATETIME(6) NULL,
    created_ip_hash BINARY(32) NULL,
    created_user_agent_hash BINARY(32) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT UK_refresh_token_families_family_uuid UNIQUE (family_uuid),
    INDEX IX_refresh_token_families_user_revoked_expiry (user_id, revoked_at, absolute_expires_at),
    INDEX IX_refresh_token_families_expiry_revoked (absolute_expires_at, revoked_at),
    CONSTRAINT FK_refresh_token_families_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    generation BIGINT NOT NULL,
    token_hmac BINARY(32) NOT NULL,
    hmac_key_id VARCHAR(32) NOT NULL,
    predecessor_token_id BIGINT NULL,
    issued_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    last_presented_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_refresh_tokens_hmac UNIQUE (hmac_key_id, token_hmac),
    CONSTRAINT UK_refresh_tokens_family_generation UNIQUE (family_id, generation),
    CONSTRAINT UK_refresh_tokens_predecessor UNIQUE (predecessor_token_id),
    INDEX IX_refresh_tokens_family_consumed_revoked (family_id, consumed_at, revoked_at),
    INDEX IX_refresh_tokens_expires_at (expires_at),
    CONSTRAINT FK_refresh_tokens_family FOREIGN KEY (family_id) REFERENCES refresh_token_families (id) ON DELETE CASCADE,
    CONSTRAINT FK_refresh_tokens_predecessor FOREIGN KEY (predecessor_token_id) REFERENCES refresh_tokens (id)
);

CREATE TABLE IF NOT EXISTS session_security_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    family_id BIGINT NULL,
    event_type VARCHAR(32) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    delivery_state VARCHAR(16) NOT NULL,
    delivery_attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NULL,
    delivered_at DATETIME(6) NULL,
    last_error_code VARCHAR(64) NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_session_security_events_family_type UNIQUE (family_id, event_type),
    INDEX IX_session_security_events_delivery_next_attempt (delivery_state, next_attempt_at),
    CONSTRAINT FK_session_security_events_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT FK_session_security_events_family FOREIGN KEY (family_id) REFERENCES refresh_token_families (id)
);
