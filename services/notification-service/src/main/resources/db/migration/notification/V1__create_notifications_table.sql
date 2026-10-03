CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    reference_type VARCHAR(255) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    status VARCHAR(255) NOT NULL
);

CREATE INDEX idx_notifications_recipient_id ON notifications(recipient_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_reference ON notifications(reference_id, reference_type);
