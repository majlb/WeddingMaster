CREATE TABLE guests (
    id BIGSERIAL PRIMARY KEY,
    wedding_couple_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    person_count INTEGER NOT NULL,
    phone VARCHAR(30),
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_guests_couple ON guests(wedding_couple_id);
