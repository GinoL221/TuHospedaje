-- ============================================================
-- Development-only demo guest (role USER) for TuHospedaje
-- Requires Flyway placeholder dev_guest_password_hash from DEV_GUEST_PASSWORD_HASH.
-- Runs only with the explicit db/dev opt-in, right after V1_9000. The preflight
-- repeats the database-name guard and validates the hash before the INSERT.

CREATE TEMPORARY TABLE dev_seed_guest_guard (
    database_name VARCHAR(64) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    CONSTRAINT chk_dev_seed_guest_database_name CHECK (
        LOWER(database_name) REGEXP '(^|_)(dev|test)(_|$)'
        AND LOWER(database_name) NOT REGEXP '(^|_)(prod|production|stage|staging|uat)(_|$)'
    ),
    CONSTRAINT chk_dev_seed_guest_bcrypt_hash CHECK (
        CHAR_LENGTH(password_hash) = 60
        AND LEFT(password_hash, 4) IN ('$2a$', '$2b$', '$2y$')
        AND SUBSTRING(password_hash, 5, 2) REGEXP '^[0-9]{2}$'
        AND CAST(SUBSTRING(password_hash, 5, 2) AS UNSIGNED) BETWEEN 10 AND 14
        AND SUBSTRING(password_hash, 7, 1) = '$'
        AND SUBSTRING(password_hash, 8) REGEXP '^[./A-Za-z0-9]{53}$'
    )
);
INSERT INTO dev_seed_guest_guard (database_name, password_hash)
VALUES (DATABASE(), '${dev_guest_password_hash}');
DROP TEMPORARY TABLE dev_seed_guest_guard;
-- ============================================================

-- Guest password is supplied at migration time; no credential is stored in source.
INSERT INTO users (first_name, last_name, email, password, role) VALUES
('Huésped', 'Demo', 'huesped@tuhospedaje.com', '${dev_guest_password_hash}', 'USER');
