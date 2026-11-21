ALTER TABLE suppliers ADD COLUMN time_zone VARCHAR(100);
ALTER TABLE bookings ADD COLUMN quantity INTEGER;
ALTER TABLE bookings ADD COLUMN service_price NUMERIC(12, 2);
ALTER TABLE bookings ADD COLUMN service_price_currency VARCHAR(3);
ALTER TABLE bookings ADD COLUMN service_pricing_type VARCHAR(20);
CREATE TABLE booking_history (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    status VARCHAR(20) NOT NULL,
    actor VARCHAR(20) NOT NULL,
    comment VARCHAR(3000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_booking_history_booking ON booking_history(booking_id, created_at, id);
