package com.terramap.adapter.in.web.dto;

import com.terramap.domain.service.GeometryValidationException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.List;

/**
 * GeoJSON Point representation adhering strictly to RFC 7946.
 * Coordinates are formatted as [longitude, latitude].
 */
@Schema(description = "GeoJSON Point object with [longitude, latitude] in EPSG:4326")
public record GeoJsonPointDto(
        @Schema(example = "Point", allowableValues = {"Point"})
        @NotNull String type,

        @Schema(example = "[-46.63, -23.55]")
        @NotEmpty List<Double> coordinates
) {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public Point toJtsPoint() {
        if (!"Point".equalsIgnoreCase(type)) {
            throw new GeometryValidationException("Geometry type must be 'Point', got: " + type);
        }
        if (coordinates == null || coordinates.size() < 2) {
            throw new GeometryValidationException("Point coordinates must contain at least [longitude, latitude]");
        }
        double lon = coordinates.get(0);
        double lat = coordinates.get(1);

        if (lon < -180.0 || lon > 180.0) {
            throw new GeometryValidationException("Longitude must be between -180 and 180, got: " + lon);
        }
        if (lat < -90.0 || lat > 90.0) {
            throw new GeometryValidationException("Latitude must be between -90 and 90, got: " + lat);
        }

        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
        point.setSRID(4326);
        return point;
    }

    public static GeoJsonPointDto fromJtsPoint(Point point) {
        if (point == null) return null;
        return new GeoJsonPointDto("Point", List.of(point.getX(), point.getY()));
    }
}
