CREATE TABLE land_parcel (
    id              UUID            PRIMARY KEY,
    title           VARCHAR(120)    NOT NULL,
    description     VARCHAR(2000),
    total_price     NUMERIC(15, 2)  NOT NULL,
    currency        VARCHAR(3)         NOT NULL DEFAULT 'BRL',
    contact_name    VARCHAR(120)    NOT NULL,
    contact_email   VARCHAR(180)    NOT NULL,
    contact_phone   VARCHAR(30),
    status          VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    boundary        geometry(Polygon, 4326) NOT NULL,
    owner_id        UUID,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT chk_total_price_positive CHECK (total_price > 0),
    CONSTRAINT chk_status_allowed       CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD')),
    CONSTRAINT chk_boundary_valid       CHECK (ST_IsValid(boundary)),
    CONSTRAINT chk_boundary_not_empty   CHECK (NOT ST_IsEmpty(boundary))
);

-- Primary spatial index: accelerates && , ST_Intersects, ST_Relate
CREATE INDEX idx_land_parcel_boundary
    ON land_parcel USING GIST (boundary);

-- Functional index on geography cast:
-- allows ST_DWithin in real metres to use index without full sequential scan
CREATE INDEX idx_land_parcel_boundary_geography
    ON land_parcel USING GIST ((boundary::geography));

CREATE INDEX idx_land_parcel_status_created
    ON land_parcel (status, created_at DESC);
