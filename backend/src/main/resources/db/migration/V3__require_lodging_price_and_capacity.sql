UPDATE lodgings
SET price_per_night = 190.00
WHERE price_per_night IS NULL;

UPDATE lodgings
SET max_guests = 4
WHERE max_guests IS NULL;

ALTER TABLE lodgings
    MODIFY price_per_night DECIMAL(10,2) NOT NULL,
    MODIFY max_guests INT NOT NULL;
