CREATE TABLE supplier_services (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    localized_content JSONB NOT NULL DEFAULT '{}'::jsonb,
    price NUMERIC(12, 2),
    image_url VARCHAR(500)
);

CREATE INDEX idx_supplier_services_supplier ON supplier_services(supplier_id);
