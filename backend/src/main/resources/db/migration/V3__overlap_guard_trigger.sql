-- Application-level validation can be defeated by two concurrent inserts.
-- This trigger is the database-level last line of defence.
CREATE OR REPLACE FUNCTION assert_no_boundary_overlap()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM land_parcel existing
        WHERE existing.id <> NEW.id
          AND existing.boundary && NEW.boundary                     -- index-backed bbox filter
          AND ST_Relate(existing.boundary, NEW.boundary, 'T********') -- interior-interior overlap
    ) THEN
        RAISE EXCEPTION 'OVERLAPPING_PARCEL'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_land_parcel_no_overlap
    BEFORE INSERT OR UPDATE OF boundary ON land_parcel
    FOR EACH ROW
    EXECUTE FUNCTION assert_no_boundary_overlap();
