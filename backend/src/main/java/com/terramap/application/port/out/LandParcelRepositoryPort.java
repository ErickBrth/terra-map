package com.terramap.application.port.out;

import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.SearchArea;

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
     * Returns parcels whose boundary intersects the search circle.
     * Uses {@code ST_DWithin(boundary::geography, center::geography, radius)} for index-backed search.
     *
     * @param searchArea the circular search area (center in EPSG:4326, radius in metres)
     * @param page       zero-based page index
     * @param size       page size
     */
    List<LandParcel> findWithinRadius(SearchArea searchArea, int page, int size);

    /**
     * Returns IDs of parcels that overlap with the candidate boundary.
     * Uses {@code ST_Relate(boundary, candidate, 'T********')} — interior-interior test.
     * Called before save to enforce the no-overlap business rule.
     */
    List<UUID> findOverlappingIds(org.locationtech.jts.geom.Polygon candidate);
}
