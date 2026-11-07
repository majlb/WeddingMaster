ALTER TABLE suppliers ALTER COLUMN category_id DROP NOT NULL;

CREATE TABLE supplier_category_assignments (
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES supplier_categories(id),
    display_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (supplier_id, category_id)
);

INSERT INTO supplier_category_assignments (supplier_id, category_id, display_order)
SELECT id, category_id, 0
FROM suppliers
WHERE category_id IS NOT NULL;

ALTER TABLE supplier_services ADD COLUMN category_id BIGINT;
UPDATE supplier_services service
SET category_id = supplier.category_id
FROM suppliers supplier
WHERE supplier.id = service.supplier_id;
ALTER TABLE supplier_services
    ADD CONSTRAINT fk_supplier_services_category
    FOREIGN KEY (category_id) REFERENCES supplier_categories(id);
ALTER TABLE supplier_services ALTER COLUMN category_id SET NOT NULL;
CREATE INDEX idx_supplier_services_category ON supplier_services(category_id);
