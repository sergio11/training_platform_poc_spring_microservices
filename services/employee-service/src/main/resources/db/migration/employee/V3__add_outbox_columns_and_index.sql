ALTER TABLE outbox_events ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ALTER COLUMN payload TYPE TEXT;
CREATE INDEX idx_outbox_events_unprocessed ON outbox_events(processed, created_at);
