ALTER TABLE reservations
    ADD COLUMN notes VARCHAR(1000) NULL,
    ADD COLUMN created_at DATETIME(6) NULL,
    ADD COLUMN created_at_derived BOOLEAN NULL;

UPDATE reservations
SET created_at = TIMESTAMP(check_in),
    created_at_derived = TRUE
WHERE created_at IS NULL;

ALTER TABLE reservations
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN created_at_derived BOOLEAN NOT NULL DEFAULT FALSE;
