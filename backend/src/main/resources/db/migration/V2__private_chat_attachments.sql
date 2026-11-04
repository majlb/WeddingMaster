CREATE TABLE chat_attachments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    uploader_id BIGINT NOT NULL REFERENCES app_users(id),
    storage_key VARCHAR(200) NOT NULL,
    file_name VARCHAR(180) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
