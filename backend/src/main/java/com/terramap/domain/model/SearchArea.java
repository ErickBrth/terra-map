package com.terramap.domain.model;

import org.locationtech.jts.geom.Point;

import java.util.Objects;

/**
 * Immutable value object representing a circular search area.
 *
 * <p>Both center and radius are validated in the constructor. The radius is
 * always expressed in real-world metres (using the PostGIS {@code geography}
 * type on the persistence side and {@code getPointResolution} on the frontend).
 */
public final class SearchArea {

    /** Maximum allowed radius, aligned with {@code terramap.search.max-radius-meters}. */
    public static final double MAX_RADIUS_METERS = 50_000.0;

    private final Point center;
    private final double radiusInMeters;

    public SearchArea(Point center, double radiusInMeters) {
        Objects.requireNonNull(center, "center must not be null");
        if (center.getSRID() != 4326) {
            throw new IllegalArgumentException(
                    "center must be in EPSG:4326, got SRID " + center.getSRID());
        }
        if (radiusInMeters <= 0) {
            throw new IllegalArgumentException(
                    "radiusInMeters must be positive, got " + radiusInMeters);
        }
        if (radiusInMeters > MAX_RADIUS_METERS) {
            throw new IllegalArgumentException(
                    "radiusInMeters exceeds maximum of " + MAX_RADIUS_METERS + ", got " + radiusInMeters);
        }
        this.center = center;
        this.radiusInMeters = radiusInMeters;
    }

    public Point getCenter() { return center; }
    public double getRadiusInMeters() { return radiusInMeters; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchArea other)) return false;
        return Double.compare(radiusInMeters, other.radiusInMeters) == 0
                && center.equals(other.center);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, radiusInMeters);
    }

    @Override
    public String toString() {
        return "SearchArea{center=" + center + ", radiusInMeters=" + radiusInMeters + "}";
    }
}
