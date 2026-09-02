package com.terramap.adapter.out.persistence;

import com.terramap.adapter.out.persistence.entity.LandParcelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for land parcel persistence.
 *
 * <p>All spatial queries use PostGIS functions via native SQL.
 * JPQL is NOT used for spatial operations because JPA 3.x has no spatial
 * predicate support — spatial logic must stay in native queries.
 */
public interface LandParcelJpaRepository extends JpaRepository<LandParcelEntity, UUID> {

    /**
     * Finds parcels whose boundary intersects a circle centred at {@code (lon, lat)}
     * with the given radius in real-world metres.
     *
     * <p>{@code ST_DWithin} on the {@code geography} cast is the correct function here:
     * it computes distances in metres on the ellipsoid and uses the functional GiST index
     * created by migration V2 ({@code idx_land_parcel_boundary_geography}).
     * Using {@code ST_Distance(...) < :radius} would force a sequential scan.
     *
     * <p>The {@code <->} operator in {@code ORDER BY} is PostGIS's KNN (k-nearest-neighbour)
     * operator: it sorts by proximity using the same spatial index, rather than
     * computing {@code ST_Distance} for every matching row and sorting that.
     */
    @Query(value = """
            SELECT *
            FROM land_parcel lp
            WHERE ST_DWithin(
                      lp.boundary::geography,
                      ST_SetSRID(ST_Point(:lon, :lat), 4326)::geography,
                      :radiusInMeters
                  )
            ORDER BY lp.boundary::geography <-> ST_SetSRID(ST_Point(:lon, :lat), 4326)::geography
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<LandParcelEntity> findWithinRadius(
            @Param("lon") double lon,
            @Param("lat") double lat,
            @Param("radiusInMeters") double radiusInMeters,
            @Param("size") int size,
            @Param("offset") int offset
    );

    /**
     * Returns UUIDs of parcels that overlap with the candidate boundary using
     * DE-9IM pattern {@code T********} (interior-interior intersection).
     *
     * <p>This is deliberately NOT {@code ST_Intersects}, which returns {@code true}
     * for adjacent parcels that only share a boundary edge.
     * The {@code &&} operator is the index-backed bounding-box pre-filter.
     */
    @Query(value = """
            SELECT lp.id::text
            FROM land_parcel lp
            WHERE lp.boundary && CAST(:candidate AS geometry)
              AND ST_Relate(lp.boundary, CAST(:candidate AS geometry), 'T********')
            """, nativeQuery = true)
    List<String> findOverlappingIds(@Param("candidate") String candidateWkt);
}
