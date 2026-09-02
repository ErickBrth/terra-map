package com.terramap.adapter.in.web.dto;

import com.terramap.domain.service.GeometryValidationException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GeoJSON Polygon representation adhering strictly to RFC 7946.
 * Coordinates are formatted as [[[longitude, latitude], ...]].
 */
@Schema(description = "GeoJSON Polygon object with EPSG:4326 coordinates")
public record GeoJsonPolygonDto(
        @Schema(example = "Polygon", allowableValues = {"Polygon"})
        @NotNull String type,

        @Schema(example = "[[[-46.63, -23.55], [-46.62, -23.55], [-46.62, -23.56], [-46.63, -23.56], [-46.63, -23.55]]]")
        @NotEmpty List<List<List<Double>>> coordinates
) {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public Polygon toJtsPolygon() {
        if (!"Polygon".equalsIgnoreCase(type)) {
            throw new GeometryValidationException("Geometry type must be 'Polygon', got: " + type);
        }
        if (coordinates == null || coordinates.isEmpty()) {
            throw new GeometryValidationException("Coordinates array must not be empty");
        }

        LinearRing shell = toRing(coordinates.get(0), "exterior");

        LinearRing[] holes = null;
        if (coordinates.size() > 1) {
            holes = new LinearRing[coordinates.size() - 1];
            for (int h = 1; h < coordinates.size(); h++) {
                holes[h - 1] = toRing(coordinates.get(h), "interior");
            }
        }

        Polygon polygon = GEOMETRY_FACTORY.createPolygon(shell, holes);
        polygon.setSRID(4326);
        return polygon;
    }

    /**
     * Converts one GeoJSON ring to a JTS {@link LinearRing}, validating that it
     * is closed (first coordinate == last) with a clear 422 before JTS itself
     * would reject it with a generic {@code IllegalArgumentException} — which
     * has no dedicated exception handler and would otherwise surface as a 400,
     * contradicting the documented "422 = invalid geometry" contract.
     */
    private static LinearRing toRing(List<List<Double>> ringCoords, String ringName) {
        if (ringCoords == null || ringCoords.size() < 4) {
            throw new GeometryValidationException(
                    "Polygon " + ringName + " ring must have at least 4 coordinates (closed ring)");
        }

        Coordinate[] coords = new Coordinate[ringCoords.size()];
        for (int i = 0; i < ringCoords.size(); i++) {
            List<Double> pt = ringCoords.get(i);
            if (pt == null || pt.size() < 2) {
                throw new GeometryValidationException("Each coordinate must contain [longitude, latitude]");
            }
            coords[i] = new Coordinate(pt.get(0), pt.get(1));
        }

        if (!Objects.equals(coords[0], coords[coords.length - 1])) {
            throw new GeometryValidationException(
                    "Polygon " + ringName + " ring must be closed (first point must equal last point)");
        }

        return GEOMETRY_FACTORY.createLinearRing(coords);
    }

    public static GeoJsonPolygonDto fromJtsPolygon(Polygon polygon) {
        if (polygon == null) return null;

        List<List<List<Double>>> allRings = new ArrayList<>();
        allRings.add(ringToCoords(polygon.getExteriorRing()));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            allRings.add(ringToCoords(polygon.getInteriorRingN(i)));
        }

        return new GeoJsonPolygonDto("Polygon", allRings);
    }

    private static List<List<Double>> ringToCoords(LineString ring) {
        List<List<Double>> coords = new ArrayList<>();
        for (Coordinate coord : ring.getCoordinates()) {
            coords.add(List.of(coord.getX(), coord.getY()));
        }
        return coords;
    }
}
