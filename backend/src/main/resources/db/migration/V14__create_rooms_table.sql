-- Salles (cahier-des-charges.md §9).
CREATE TABLE rooms (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id  BIGINT       NOT NULL REFERENCES tenants (id),
    name       VARCHAR(100) NOT NULL,
    capacity   INTEGER,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_rooms_school_id ON rooms (school_id);

ALTER TABLE rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE rooms FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON rooms
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
