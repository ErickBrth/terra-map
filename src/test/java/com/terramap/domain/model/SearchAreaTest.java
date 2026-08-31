package com.terramap.domain.model;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchAreaTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private static Point point4326(double lon, double lat) {
        Point p = GF.createPoint(new Coordinate(lon, lat));
        p.setSRID(4326);
        return p;
    }

    @Test
    void createsValidSearchArea() {
        Point center = point4326(-46.63, -23.55);

        SearchArea area = new SearchArea(center, 1500.0);

        assertThat(area.getCenter()).isEqualTo(center);
        assertThat(area.getRadiusInMeters()).isEqualTo(1500.0);
    }

    @Test
    void rejectsNullCenter() {
        assertThatThrownBy(() -> new SearchArea(null, 1000))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsCenterWithWrongSrid() {
        Point wrongSrid = GF.createPoint(new Coordinate(-46.63, -23.55));
        wrongSrid.setSRID(3857); // Web Mercator, not WGS84

        assertThatThrownBy(() -> new SearchArea(wrongSrid, 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4326");
    }

    @Test
    void rejectsZeroRadius() {
        assertThatThrownBy(() -> new SearchArea(point4326(0, 0), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeRadius() {
        assertThatThrownBy(() -> new SearchArea(point4326(0, 0), -50))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsRadiusAboveMaximum() {
        assertThatThrownBy(() -> new SearchArea(point4326(0, 0), SearchArea.MAX_RADIUS_METERS + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum");
    }

    @Test
    void acceptsRadiusExactlyAtMaximum() {
        SearchArea area = new SearchArea(point4326(0, 0), SearchArea.MAX_RADIUS_METERS);

        assertThat(area.getRadiusInMeters()).isEqualTo(SearchArea.MAX_RADIUS_METERS);
    }

    @Test
    void equalAreasAreEqual() {
        Point center = point4326(-46.63, -23.55);
        SearchArea a = new SearchArea(center, 1000);
        SearchArea b = new SearchArea(center, 1000);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
