package com.terramap.application.port.out;

import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.ParcelStatus;
import com.terramap.domain.model.SearchArea;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port (repository abstraction) for land parcel persistence.
 *
 * <p>The domain and application layers speak only in terms of {@link LandParcel}.
 * The JPA entity, SQL queries, and PostGIS functions live exclusively in the
 * adapter that implements this interface.
 */
public interface LandParcelRepositoryPort {

    /** Persists a new or updated parcel and returns the managed instance. */
    LandParcel save(LandParcel parcel);

    /** Finds a parcel by its UUID. */
    Optional<LandParcel> findById(UUID id);

    /**
     * Returns parcels whose boundary intersects the search circle and match the
     * optional filters, ordered by distance from the center (closest first).
     * Either filter may be {@code null}, meaning "no constraint on that field."
     *
     * <p>Uses {@code ST_DWithin(boundary::geography, center::geography, radius)},
     * backed by a functional GiST index on the geography cast — never
     * {@code ST_Distance(...) < radius}, which would force a sequential scan.
     */
    List<LandParcel> findWithinRadius(SearchArea searchArea, BigDecimal maxPrice, ParcelStatus status, int page, int size);

    /**
     * Returns IDs of existing parcels that overlap with the candidate boundary.
     * Called before save to enforce the no-overlap business rule.
     *
     * <p>Uses PostGIS {@code ST_Relate(boundary, candidate, 'T********')} — the
     * DE-9IM pattern for "interiors intersect" — never {@code ST_Intersects}.
     *
     * <pre>
     * Situation                     | ST_Intersects | ST_Overlaps | ST_Relate('T********')
     * ------------------------------|---------------|-------------|------------------------
     * Adjacent parcels (share edge) | true  (wrong) | false       | false  correct
     * Partial overlap               | true          | true        | true   correct
     * A entirely inside B           | true          | false (!)   | true   correct
     * Identical geometries          | true          | false (!)   | true   correct
     * </pre>
     *
     * Adjacent parcels legitimately share a border in any real subdivision, so
     * {@code ST_Intersects} would make the system unusable. {@code ST_Overlaps}
     * alone is also wrong: it misses one polygon fully contained in another,
     * the most likely fraud case. This same rule is enforced twice — here (for
     * a clear 409 response) and as a {@code BEFORE INSERT} database trigger,
     * which is the actual guarantee against two concurrent registrations racing
     * past this check at the same time.
     */
    List<UUID> findOverlappingIds(Polygon candidate);
}
