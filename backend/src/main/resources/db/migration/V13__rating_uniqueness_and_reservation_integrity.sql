-- Validate all legacy rows before applying any schema or data changes.
DELIMITER $$
BEGIN NOT ATOMIC
    IF EXISTS (SELECT 1 FROM ratings GROUP BY lodging_id, user_id HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V13 blocked: duplicate ratings require manual resolution';
    END IF;
    IF EXISTS (SELECT 1 FROM reservations WHERE check_out <= check_in) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V13 blocked: invalid reservation dates require manual resolution';
    END IF;
END$$
DELIMITER ;

UPDATE reservations SET version = 0 WHERE version IS NULL;

ALTER TABLE reservations MODIFY version BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX IF NOT EXISTS UK_ratings_lodging_user ON ratings (lodging_id, user_id);
ALTER TABLE reservations ADD CONSTRAINT IF NOT EXISTS CK_reservations_date_range CHECK (check_out > check_in);
