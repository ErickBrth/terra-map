package com.terramap.adapter.in.web.dto;

import com.terramap.domain.service.GeometryValidationException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;

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

        List<List<Double>> exteriorRingCoords = coordinates.get(0);
        if (exteriorRingCoords == null || exteriorRingCoords.size() < 4) {
            throw new GeometryValidationException("Polygon exterior ring must have at least 4 coordinates (closed ring)");
        }

        Coordinate[] coords = new Coordinate[exteriorRingCoords.size()];
        for (int i = 0; i < exteriorRingCoords.size(); i++) {
            List<Double> pt = exteriorRingCoords.get(i);
            if (pt == null || pt.size() < 2) {
                throw new GeometryValidationException("Each coordinate must contain [longitude, latitude]");
            }
            double lon = pt.get(0);
            double lat = pt.get(1);
            coords[i] = new Coordinate(lon, lat);
        }

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coords);

        // Process interior rings (holes) if any
        LinearRing[] holes = null;
        if (coordinates.size() > 1) {
            holes = new LinearRing[coordinates.size() - 1];
            for (int h = 1; h < coordinates.size(); h++) {
                List<List<Double>> holeCoordsList = coordinates.get(h);
                Coordinate[] holeCoords = new Coordinate[holeCoordsList.size()];
                for (int i = 0; i < holeCoordsList.size(); i++) {
                    List<Double> pt = holeCoordsList.get(i);
                    holeCoords[i] = new Coordinate(pt.get(0), pt.get(1));
                }
                holes[h - 1] = GEOMETRY_FACTORY.createLinearRing(holeCoords);
            }
        }

        Polygon polygon = GEOMETRY_FACTORY.createPolygon(shell, holes);
        polygon.setSRID(4326);
        return polygon;
    }

    public static GeoJsonPolygonDto fromJtsPolygon(Polygon polygon) {
        if (polygon == null) return null;

        List<List<List<Double>>> allRings = new ArrayList<>();

        // Exterior ring
        LineString exteriorRing = polygon.getExteriorRing();
        List<List<Double>> exteriorCoords = new ArrayList<>();
        for (Coordinate coord : exteriorRing.getCoordinates()) {
            exteriorCoords.add(List.of(coord.getX(), coord.getY()));
        }
        allRings.add(exteriorCoords);

        // Holes
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            LineString interiorRing = polygon.getInteriorRingN(i);
            List<List<Double>> interiorCoords = new ArrayList<>();
            for (Coordinate coord : interiorRing.getCoordinates()) {
                interiorCoords.add(List.of(coord.getX(), coord.getY()));
            }
            allRings.add(interiorCoords);
        }

        return new GeoJsonPolygonDto("Polygon", allRings);
    }
}
