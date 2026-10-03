ALTER TABLE notifications
ADD CONSTRAINT uk_notification_reference_type
UNIQUE (reference_id, reference_type, type);
