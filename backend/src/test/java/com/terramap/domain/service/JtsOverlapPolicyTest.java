package com.terramap.domain.service;

import com.terramap.domain.model.ContactInfo;
import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.Money;
import com.terramap.support.GeometryFixtures;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the decision table documented on {@link OverlapPolicy}:
 * only interior-interior intersection (DE-9IM {@code T********}) counts as overlap.
 */
class JtsOverlapPolicyTest {

    private final JtsOverlapPolicy policy = new JtsOverlapPolicy();
    private static final Money PRICE = new Money(BigDecimal.TEN, "BRL");
    private static final ContactInfo CONTACT = new ContactInfo("Jane Doe", "jane@example.com", null);

    private static LandParcel parcelWithBoundary(Polygon boundary) {
        return LandParcel.create("Existing parcel", null, PRICE, CONTACT, boundary);
    }

    @Test
    void detectsPartialOverlap() {
        LandParcel existing = parcelWithBoundary(GeometryFixtures.saoPauloParcelA());
        Polygon candidate = GeometryFixtures.saoPauloParcelOverlappingA();

        assertThat(policy.hasOverlap(candidate, List.of(existing))).isTrue();
    }

    @Test
    void detectsCandidateFullyContainedInExisting() {
        LandParcel existing = parcelWithBoundary(GeometryFixtures.saoPauloParcelA());
        Polygon candidate = GeometryFixtures.saoPauloParcelContainedInA();

        assertThat(policy.hasOverlap(candidate, List.of(existing))).isTrue();
    }

    @Test
    void detectsExistingFullyContainedInCandidate() {
        // symmetric case: the *new* parcel is the larger one, containing the existing smaller parcel
        LandParcel existingSmall = parcelWithBoundary(GeometryFixtures.saoPauloParcelContainedInA());
        Polygon largeCandidate = GeometryFixtures.saoPauloParcelA();

        assertThat(policy.hasOverlap(largeCandidate, List.of(existingSmall))).isTrue();
    }

    @Test
    void detectsIdenticalGeometries() {
        LandParcel existing = parcelWithBoundary(GeometryFixtures.saoPauloParcelA());
        Polygon candidate = GeometryFixtures.saoPauloParcelIdenticalToA();

        assertThat(policy.hasOverlap(candidate, List.of(existing))).isTrue();
    }

    @Test
    void allowsParcelsSharingOnlyABorder() {
        LandParcel existing = parcelWithBoundary(GeometryFixtures.saoPauloParcelA());
        Polygon adjacent = GeometryFixtures.saoPauloParcelAdjacentToA();

        assertThat(policy.hasOverlap(adjacent, List.of(existing))).isFalse();
    }

    @Test
    void allowsParcelsTouchingAtASinglePoint() {
        LandParcel existing = parcelWithBoundary(GeometryFixtures.saoPauloParcelA());
        Polygon touchingCorner = GeometryFixtures.saoPauloParcelTouchingACorner();

        assertThat(policy.hasOverlap(touchingCorner, List.of(existing))).isFalse();
    }

    @Test
    void allowsDisjointParcels() {
        LandParcel existing = parcelWithBoundary(GeometryFixtures.saoPauloParcelA());
        Polygon farAway = GeometryFixtures.rioDeJaneiroParcel();

        assertThat(policy.hasOverlap(farAway, List.of(existing))).isFalse();
    }

    @Test
    void returnsFalseWhenNoExistingParcels() {
        Polygon candidate = GeometryFixtures.saoPauloParcelA();

        assertThat(policy.hasOverlap(candidate, List.of())).isFalse();
    }

    @Test
    void detectsOverlapAgainstAnyMemberOfTheExistingSet() {
        LandParcel farAway = parcelWithBoundary(GeometryFixtures.rioDeJaneiroParcel());
        LandParcel overlapping = parcelWithBoundary(GeometryFixtures.saoPauloParcelA());
        Polygon candidate = GeometryFixtures.saoPauloParcelOverlappingA();

        assertThat(policy.hasOverlap(candidate, List.of(farAway, overlapping))).isTrue();
    }
}
