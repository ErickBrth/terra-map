package com.terramap.support;

import org.locationtech.jts.geom.*;

/**
 * Reusable JTS geometries for tests. All polygons are in EPSG:4326.
 *
 * <p>Naming follows the relationship to {@link #saoPauloParcelA()}, which acts as the
 * reference parcel that every other fixture is tested against.
 */
public final class GeometryFixtures {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private GeometryFixtures() {}

    /** Creates a polygon in EPSG:4326 from lon/lat coordinates. The ring must already be closed. */
    public static Polygon createPolygon(double[][] coords) {
        Coordinate[] jtsCoords = new Coordinate[coords.length];
        for (int i = 0; i < coords.length; i++) {
            jtsCoords[i] = new Coordinate(coords[i][0], coords[i][1]);
        }
        LinearRing shell = GF.createLinearRing(jtsCoords);
        Polygon polygon = GF.createPolygon(shell);
        polygon.setSRID(4326);
        return polygon;
    }

    /** Creates a point in EPSG:4326. */
    public static Point createPoint(double lon, double lat) {
        Point point = GF.createPoint(new Coordinate(lon, lat));
        point.setSRID(4326);
        return point;
    }

    /** Square parcel in Sao Paulo (approx. 1km box). Reference parcel for all "vs A" fixtures. */
    public static Polygon saoPauloParcelA() {
        return createPolygon(new double[][]{
                {-46.635, -23.555},
                {-46.625, -23.555},
                {-46.625, -23.545},
                {-46.635, -23.545},
                {-46.635, -23.555}
        });
    }

    /**
     * Adjacent parcel sharing the exact eastern border of Parcel A (-46.625).
     * Must NOT be considered overlapping under DE-9IM T********!
     */
    public static Polygon saoPauloParcelAdjacentToA() {
        return createPolygon(new double[][]{
                {-46.625, -23.555},
                {-46.615, -23.555},
                {-46.615, -23.545},
                {-46.625, -23.545},
                {-46.625, -23.555}
        });
    }

    /** Touches Parcel A at exactly one corner point (-46.625, -23.545). Must NOT overlap. */
    public static Polygon saoPauloParcelTouchingACorner() {
        return createPolygon(new double[][]{
                {-46.625, -23.545},
                {-46.615, -23.545},
                {-46.615, -23.535},
                {-46.625, -23.535},
                {-46.625, -23.545}
        });
    }

    /** Partially overlapping parcel with Parcel A. */
    public static Polygon saoPauloParcelOverlappingA() {
        return createPolygon(new double[][]{
                {-46.630, -23.550},
                {-46.620, -23.550},
                {-46.620, -23.540},
                {-46.630, -23.540},
                {-46.630, -23.550}
        });
    }

    /** Smaller parcel completely contained inside Parcel A. */
    public static Polygon saoPauloParcelContainedInA() {
        return createPolygon(new double[][]{
                {-46.632, -23.552},
                {-46.628, -23.552},
                {-46.628, -23.548},
                {-46.632, -23.548},
                {-46.632, -23.552}
        });
    }

    /** Identical geometry to Parcel A. */
    public static Polygon saoPauloParcelIdenticalToA() {
        return saoPauloParcelA();
    }

    /** Far away parcel in Rio de Janeiro (approx 350 km away). Disjoint from A. */
    public static Polygon rioDeJaneiroParcel() {
        return createPolygon(new double[][]{
                {-43.200, -22.910},
                {-43.190, -22.910},
                {-43.190, -22.900},
                {-43.200, -22.900},
                {-43.200, -22.910}
        });
    }

    /**
     * Self-intersecting "bowtie" polygon: an invalid OGC geometry.
     * Built directly on the JTS geometry factory because {@code createLinearRing}
     * does not reject self-intersection at construction time -- only {@code Polygon#isValid()} does.
     */
    public static Polygon selfIntersectingBowtie() {
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(-46.63, -23.55),
                new Coordinate(-46.62, -23.54),
                new Coordinate(-46.63, -23.54),
                new Coordinate(-46.62, -23.55),
                new Coordinate(-46.63, -23.55) // closes the ring, but edges cross in the middle
        };
        LinearRing shell = GF.createLinearRing(coords);
        Polygon polygon = GF.createPolygon(shell);
        polygon.setSRID(4326);
        return polygon;
    }

    /** A triangle-like ring with only 3 distinct points (4 including the closing point) -- the legal minimum. */
    public static Polygon minimalValidTriangle() {
        return createPolygon(new double[][]{
                {-46.63, -23.55},
                {-46.62, -23.55},
                {-46.625, -23.54},
                {-46.63, -23.55}
        });
    }

    /** A polygon with a very high vertex count, for boundary/DoS-style validation tests. */
    public static Polygon polygonWithVertexCount(int vertexCount) {
        if (vertexCount < 4) {
            throw new IllegalArgumentException("vertexCount must be >= 4 to form a closed ring");
        }
        double[][] coords = new double[vertexCount][2];
        double centerLon = -46.63;
        double centerLat = -23.55;
        double radius = 0.01;
        int segments = vertexCount - 1; // last point closes the ring
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            coords[i][0] = centerLon + radius * Math.cos(angle);
            coords[i][1] = centerLat + radius * Math.sin(angle);
        }
        coords[vertexCount - 1] = coords[0]; // close the ring
        return createPolygon(coords);
    }
}
