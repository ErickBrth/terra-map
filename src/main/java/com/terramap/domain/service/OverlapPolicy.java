package com.terramap.domain.service;

import com.terramap.domain.model.LandParcel;
import org.locationtech.jts.geom.Polygon;

import java.util.Collection;

/**
 * Strategy defining the overlap rule between land parcels.
 *
 * <p>The chosen implementation uses DE-9IM pattern {@code T********}:
 * it returns {@code true} only when the <em>interiors</em> of two polygons
 * intersect — which is exactly "the areas overlap", ignoring shared borders.
 *
 * <p>This is deliberately NOT {@code ST_Intersects}, which returns {@code true}
 * even for parcels that only share a boundary edge (adjacent neighbours).
 * Blocking neighbours would make the system unusable in any real subdivision.
 *
 * <p>{@code ST_Overlaps} alone is also wrong: it returns {@code false} when
 * one polygon is entirely contained in another — the most common fraud case.
 *
 * <p>DE-9IM matrix reference:
 * <pre>
 * Situation                     | ST_Intersects | ST_Overlaps | ST_Relate('T********')
 * ------------------------------|---------------|-------------|------------------------
 * Adjacent parcels (share edge) | true  (wrong) | false       | false  ✓
 * Partial overlap               | true          | true        | true   ✓
 * A entirely inside B           | true          | false(!)    | true   ✓
 * Identical geometries          | true          | false(!)    | true   ✓
 * </pre>
 */
public interface OverlapPolicy {

    /**
     * Returns {@code true} if {@code candidate} overlaps with any parcel in {@code existing}.
     *
     * @param candidate the new boundary being registered
     * @param existing  parcels already persisted (may be a pre-filtered subset)
     */
    boolean hasOverlap(Polygon candidate, Collection<LandParcel> existing);
}
