package com.terramap.domain.service;

import com.terramap.domain.model.LandParcel;
import org.locationtech.jts.geom.Polygon;

/**
 * JTS-based implementation of {@link GeometryValidator}.
 *
 * <p>Validates all invariants that the database would also enforce via
 * {@code CHECK (ST_IsValid(boundary))} and {@code geometry(Polygon, 4326)}.
 * Running this check before the database round-trip gives richer error messages.
 */
public class JtsGeometryValidator implements GeometryValidator {

    private static final int MIN_POINTS = 4; // closed ring: first == last, so min 4 coords
    private static final int MAX_VERTICES = LandParcel.MAX_VERTICES;

    @Override
    public void validate(Polygon polygon) {
        if (polygon == null) {
            throw new GeometryValidationException("boundary must not be null");
        }
        if (polygon.getSRID() != 4326) {
            throw new GeometryValidationException(
                    "boundary must be in EPSG:4326, got SRID " + polygon.getSRID());
        }
        if (polygon.isEmpty()) {
            throw new GeometryValidationException("boundary must not be empty");
        }

        int numPoints = polygon.getExteriorRing().getNumPoints();
        if (numPoints < MIN_POINTS) {
            throw new GeometryValidationException(
                    "boundary ring must have at least " + MIN_POINTS + " coordinates (including the closing point), got " + numPoints);
        }
        if (numPoints > MAX_VERTICES) {
            throw new GeometryValidationException(
                    "boundary must not exceed " + MAX_VERTICES + " vertices, got " + numPoints);
        }
        if (!polygon.getExteriorRing().isClosed()) {
            throw new GeometryValidationException("boundary exterior ring must be closed (first point == last point)");
        }
        if (!polygon.isValid()) {
            throw new GeometryValidationException(
                    "boundary is not a valid geometry (e.g. self-intersection / bowtie polygon)");
        }
    }
}
