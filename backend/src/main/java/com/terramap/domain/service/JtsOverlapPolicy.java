package com.terramap.domain.service;

import com.terramap.domain.model.LandParcel;
import org.locationtech.jts.geom.Polygon;

import java.util.Collection;

/**
 * JTS-based implementation of {@link OverlapPolicy}.
 *
 * <p>Uses {@link Polygon#relate(org.locationtech.jts.geom.Geometry, String)} with
 * the DE-9IM pattern {@code "T********"} to test only interior-interior intersection.
 *
 * <p>This mirrors exactly what the database trigger {@code trg_land_parcel_no_overlap}
 * does via {@code ST_Relate(existing.boundary, NEW.boundary, 'T********')}.
 * Having both layers protects against TOCTOU race conditions on concurrent inserts.
 */
public class JtsOverlapPolicy implements OverlapPolicy {

    private static final String DE9IM_INTERIOR_OVERLAP = "T********";

    @Override
    public boolean hasOverlap(Polygon candidate, Collection<LandParcel> existing) {
        return existing.stream()
                .anyMatch(parcel -> candidate.relate(parcel.getBoundary(), DE9IM_INTERIOR_OVERLAP));
    }
}
