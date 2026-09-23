CREATE TABLE admin_invariant_lock (
    id BIGINT NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO admin_invariant_lock (id) VALUES (1);

CREATE INDEX IX_users_role_enabled_id ON users (role, enabled, id);
