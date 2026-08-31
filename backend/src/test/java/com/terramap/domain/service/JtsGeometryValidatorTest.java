package com.terramap.domain.service;

import com.terramap.support.GeometryFixtures;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JtsGeometryValidatorTest {

    private final JtsGeometryValidator validator = new JtsGeometryValidator();

    @Test
    void acceptsAValidPolygon() {
        assertThatCode(() -> validator.validate(GeometryFixtures.saoPauloParcelA()))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsMinimalValidTriangle() {
        assertThatCode(() -> validator.validate(GeometryFixtures.minimalValidTriangle()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullPolygon() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(GeometryValidationException.class)
                .hasMessageContaining("null");
    }

    @Test
    void rejectsPolygonWithWrongSrid() {
        Polygon wrongSrid = GeometryFixtures.saoPauloParcelA();
        wrongSrid.setSRID(3857);

        assertThatThrownBy(() -> validator.validate(wrongSrid))
                .isInstanceOf(GeometryValidationException.class)
                .hasMessageContaining("4326");
    }

    @Test
    void rejectsSelfIntersectingBowtiePolygon() {
        assertThatThrownBy(() -> validator.validate(GeometryFixtures.selfIntersectingBowtie()))
                .isInstanceOf(GeometryValidationException.class)
                .hasMessageContaining("valid geometry");
    }

    @Test
    void rejectsPolygonExceedingMaxVertices() {
        Polygon tooManyVertices = GeometryFixtures.polygonWithVertexCount(1002);

        assertThatThrownBy(() -> validator.validate(tooManyVertices))
                .isInstanceOf(GeometryValidationException.class)
                .hasMessageContaining("1000");
    }

    @Test
    void acceptsPolygonAtExactlyMaxVertices() {
        Polygon atLimit = GeometryFixtures.polygonWithVertexCount(1000);

        assertThatCode(() -> validator.validate(atLimit)).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyPolygon() {
        Polygon empty = new org.locationtech.jts.geom.GeometryFactory(
                new org.locationtech.jts.geom.PrecisionModel(), 4326).createPolygon();
        empty.setSRID(4326);

        assertThatThrownBy(() -> validator.validate(empty))
                .isInstanceOf(GeometryValidationException.class);
    }
}
