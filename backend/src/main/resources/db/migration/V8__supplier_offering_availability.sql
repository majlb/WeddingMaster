ALTER TABLE bookings
    ADD COLUMN service_offering_id BIGINT REFERENCES supplier_services(id) ON DELETE SET NULL;

CREATE INDEX idx_booking_service_offering_date ON bookings(service_offering_id, event_date, status);

CREATE TABLE supplier_availability_blocks (
    id BIGSERIAL PRIMARY KEY,
    service_offering_id BIGINT NOT NULL REFERENCES supplier_services(id) ON DELETE CASCADE,
    blocked_date DATE NOT NULL,
    booking_id BIGINT UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT uq_offering_blocked_date UNIQUE (service_offering_id, blocked_date)
);

CREATE INDEX idx_availability_offering_date ON supplier_availability_blocks(service_offering_id, blocked_date);
