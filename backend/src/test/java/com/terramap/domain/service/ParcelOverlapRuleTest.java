package com.terramap.domain.service;

import com.terramap.support.GeometryFixtures;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the overlap business rule directly against JTS's {@code relate()},
 * using the DE-9IM pattern {@code "T********"} (interior-interior intersection).
 *
 * <p>This is the same rule enforced by the database — see
 * {@link com.terramap.application.port.out.LandParcelRepositoryPort#findOverlappingIds}
 * for the full DE-9IM decision table and its rationale. Testing it here, in plain
 * Java against JTS, proves the rule is correct in milliseconds and without a
 * database — the slower {@code LandParcelPersistenceAdapterIT} then proves the
 * equivalent SQL (executed by the real adapter and the trigger) agrees with it.
 */
class ParcelOverlapRuleTest {

    private static final String DE9IM_INTERIOR_OVERLAP = "T********";

    private static boolean overlaps(Polygon candidate, Polygon existing) {
        return candidate.relate(existing, DE9IM_INTERIOR_OVERLAP);
    }

    @Test
    void detectsPartialOverlap() {
        assertThat(overlaps(GeometryFixtures.saoPauloParcelOverlappingA(), GeometryFixtures.saoPauloParcelA()))
                .isTrue();
    }

    @Test
    void detectsCandidateFullyContainedInExisting() {
        assertThat(overlaps(GeometryFixtures.saoPauloParcelContainedInA(), GeometryFixtures.saoPauloParcelA()))
                .isTrue();
    }

    @Test
    void detectsExistingFullyContainedInCandidate() {
        // symmetric case: the *new* parcel is the larger one, containing the existing smaller parcel
        assertThat(overlaps(GeometryFixtures.saoPauloParcelA(), GeometryFixtures.saoPauloParcelContainedInA()))
                .isTrue();
    }

    @Test
    void detectsIdenticalGeometries() {
        assertThat(overlaps(GeometryFixtures.saoPauloParcelIdenticalToA(), GeometryFixtures.saoPauloParcelA()))
                .isTrue();
    }

    @Test
    void allowsParcelsSharingOnlyABorder() {
        assertThat(overlaps(GeometryFixtures.saoPauloParcelAdjacentToA(), GeometryFixtures.saoPauloParcelA()))
                .isFalse();
    }

    @Test
    void allowsParcelsTouchingAtASinglePoint() {
        assertThat(overlaps(GeometryFixtures.saoPauloParcelTouchingACorner(), GeometryFixtures.saoPauloParcelA()))
                .isFalse();
    }

    @Test
    void allowsDisjointParcels() {
        assertThat(overlaps(GeometryFixtures.rioDeJaneiroParcel(), GeometryFixtures.saoPauloParcelA()))
                .isFalse();
    }
}
