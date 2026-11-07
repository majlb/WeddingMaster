ALTER TABLE suppliers
    ADD COLUMN contact_email VARCHAR(180),
    ADD COLUMN whatsapp VARCHAR(30),
    ADD COLUMN facebook VARCHAR(500),
    ADD COLUMN instagram VARCHAR(500),
    ADD COLUMN tiktok VARCHAR(500);

ALTER TABLE user_notifications
    ALTER COLUMN booking_id DROP NOT NULL,
    ADD COLUMN collaboration_id BIGINT REFERENCES supplier_collaborations(id);
