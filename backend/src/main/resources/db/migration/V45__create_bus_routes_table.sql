-- Lignes de bus (cahier-des-charges.md §19.2).
CREATE TABLE bus_routes (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id   BIGINT      NOT NULL REFERENCES tenants (id),
    label       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bus_routes_school_id ON bus_routes (school_id);

ALTER TABLE bus_routes ENABLE ROW LEVEL SECURITY;
ALTER TABLE bus_routes FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON bus_routes
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
