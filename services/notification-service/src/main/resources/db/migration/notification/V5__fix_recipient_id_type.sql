ALTER TABLE notifications ALTER COLUMN recipient_id TYPE BIGINT USING recipient_id::BIGINT;
