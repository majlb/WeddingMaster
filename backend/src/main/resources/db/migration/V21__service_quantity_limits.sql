ALTER TABLE supplier_services ADD COLUMN minimum_amount INTEGER NOT NULL DEFAULT 1;
ALTER TABLE supplier_services ADD COLUMN maximum_amount INTEGER NOT NULL DEFAULT 1;
ALTER TABLE supplier_services ADD CONSTRAINT chk_supplier_service_amounts
    CHECK (minimum_amount >= 1 AND maximum_amount >= minimum_amount);
