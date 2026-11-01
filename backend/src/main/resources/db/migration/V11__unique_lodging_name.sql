-- Ensure unique lodging names to prevent duplicate creation races
ALTER TABLE lodgings ADD CONSTRAINT UK_lodgings_name UNIQUE (name);
