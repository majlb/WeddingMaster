CREATE TABLE supplier_collaborations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    created_by_supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_collaboration_created_at ON supplier_collaborations(created_at);

CREATE TABLE collaboration_members (
    id BIGSERIAL PRIMARY KEY,
    collaboration_id BIGINT NOT NULL REFERENCES supplier_collaborations(id) ON DELETE CASCADE,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_collaboration_member UNIQUE (collaboration_id, supplier_id)
);
CREATE INDEX idx_collaboration_member_supplier_status ON collaboration_members(supplier_id, status);

CREATE TABLE collaboration_items (
    id BIGSERIAL PRIMARY KEY,
    collaboration_id BIGINT NOT NULL REFERENCES supplier_collaborations(id) ON DELETE CASCADE,
    supplier_service_id BIGINT NOT NULL REFERENCES supplier_services(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_collaboration_service UNIQUE (collaboration_id, supplier_service_id)
);
CREATE INDEX idx_collaboration_item_collaboration ON collaboration_items(collaboration_id);
