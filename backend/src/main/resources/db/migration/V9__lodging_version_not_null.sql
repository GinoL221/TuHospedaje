UPDATE lodgings
SET version = 0
WHERE version IS NULL;

ALTER TABLE lodgings
    MODIFY version BIGINT NOT NULL DEFAULT 0;
