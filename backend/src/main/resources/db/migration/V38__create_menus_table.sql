-- Menus du jour (cahier-des-charges.md §19.1). special_diet_description couvre les "régimes
-- alimentaires particuliers" comme une variante du même menu ce jour-là, pas un profil
-- allergène par élève (donnée de santé, hors périmètre de cette passe).
CREATE TABLE menus (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_id                   BIGINT      NOT NULL REFERENCES tenants (id),
    date                        DATE        NOT NULL,
    main_description            VARCHAR(1000) NOT NULL,
    special_diet_description    VARCHAR(1000),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (school_id, date)
);

CREATE INDEX idx_menus_school_id ON menus (school_id);

ALTER TABLE menus ENABLE ROW LEVEL SECURITY;
ALTER TABLE menus FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON menus
    USING (school_id = current_setting('app.tenant_id', true)::bigint);
