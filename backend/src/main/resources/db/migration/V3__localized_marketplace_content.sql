ALTER TABLE supplier_categories
    ADD COLUMN translations JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE suppliers
    ADD COLUMN localized_content JSONB NOT NULL DEFAULT '{}'::jsonb;
