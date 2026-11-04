CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE supplier_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(90) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(300),
    active BOOLEAN NOT NULL
);

CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES app_users(id),
    category_id BIGINT NOT NULL REFERENCES supplier_categories(id),
    business_name VARCHAR(150) NOT NULL,
    slug VARCHAR(140) NOT NULL UNIQUE,
    location VARCHAR(180) NOT NULL,
    description VARCHAR(4000),
    services VARCHAR(500),
    availability VARCHAR(8000),
    starting_price NUMERIC(12, 2),
    currency VARCHAR(3),
    contact_phone VARCHAR(30),
    website VARCHAR(180),
    rating DOUBLE PRECISION,
    review_count INTEGER,
    profile_visits BIGINT NOT NULL,
    approved BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL
);
CREATE INDEX idx_supplier_category_approved ON suppliers(category_id, approved);
CREATE INDEX idx_supplier_location ON suppliers(location);

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES app_users(id),
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    event_date DATE NOT NULL,
    notes VARCHAR(3000),
    status VARCHAR(20) NOT NULL,
    quoted_amount_cents BIGINT,
    currency VARCHAR(3),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_booking_customer_status ON bookings(customer_id, status);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    stripe_payment_intent_id VARCHAR(100) UNIQUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    sender_id BIGINT NOT NULL REFERENCES app_users(id),
    body VARCHAR(4000),
    attachment_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_message_booking_created ON messages(booking_id, created_at);

CREATE TABLE checklist_items (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES app_users(id),
    title VARCHAR(180) NOT NULL,
    category_slug VARCHAR(60),
    due_date DATE,
    completed BOOLEAN NOT NULL,
    notes VARCHAR(2000),
    contract_details VARCHAR(500),
    payment_status VARCHAR(40),
    booking_id BIGINT REFERENCES bookings(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_checklist_customer ON checklist_items(customer_id);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),
    author_id BIGINT NOT NULL REFERENCES app_users(id),
    rating INTEGER NOT NULL,
    comment VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_review_booking_author UNIQUE(booking_id, author_id)
);

CREATE TABLE supplier_media (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    url VARCHAR(500) NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    caption VARCHAR(180),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
