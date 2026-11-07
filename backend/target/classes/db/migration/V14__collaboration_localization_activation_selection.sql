ALTER TABLE supplier_collaborations
    ADD COLUMN localized_names JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE collaboration_selections (
    id BIGSERIAL PRIMARY KEY,
    collaboration_id BIGINT NOT NULL REFERENCES supplier_collaborations(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_collaboration_selection UNIQUE (collaboration_id, customer_id)
);
CREATE INDEX idx_collaboration_selection_customer ON collaboration_selections(customer_id);
