package com.terramap.domain.service;

import org.locationtech.jts.geom.Polygon;

/**
 * Port for validating that a polygon is geometrically correct.
 *
 * <p>The domain defines the contract; the implementation lives here too
 * (JTS-based, no Spring dependency) because geometry validation is pure
 * business logic and must be testable without a database.
 *
 * <p>PostGIS enforces the same rules via {@code CHECK (ST_IsValid(boundary))},
 * but having the check at the application layer gives better error messages
 * and avoids a round-trip to the database.
 */
public interface GeometryValidator {

    /**
     * Validates the polygon and throws {@link GeometryValidationException}
     * if any invariant is violated.
     *
     * @param polygon the candidate boundary in EPSG:4326
     * @throws GeometryValidationException if the polygon is invalid
     */
    void validate(Polygon polygon);
}
