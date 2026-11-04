CREATE TABLE user_notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES app_users(id),
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    type VARCHAR(40) NOT NULL,
    actor_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notification_recipient_created
    ON user_notifications(recipient_id, created_at DESC);
CREATE INDEX idx_notification_recipient_read
    ON user_notifications(recipient_id, read_at);
