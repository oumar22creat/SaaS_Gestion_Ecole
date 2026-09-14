-- Arrêts d'une ligne de bus, ordonnés (cahier-des-charges.md §19.2).
CREATE TABLE bus_stops (
    id              BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id       BIGINT      NOT NULL REFERENCES tenants (id),
    bus_route_id    BIGINT      NOT NULL REFERENCES bus_routes (id),
    name            VARCHAR(255) NOT NULL,
    sequence_order  INTEGER     NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bus_stops_school_id ON bus_stops (school_id);
CREATE INDEX idx_bus_stops_route_id ON bus_stops (bus_route_id);

ALTER TABLE bus_stops ENABLE ROW LEVEL SECURITY;
ALTER TABLE bus_stops FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON bus_stops
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
