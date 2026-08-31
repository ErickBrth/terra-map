package com.terramap.domain.model;

import com.terramap.support.GeometryFixtures;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LandParcelTest {

    private static final Money PRICE = new Money(new BigDecimal("250000.00"), "BRL");
    private static final ContactInfo CONTACT = new ContactInfo("Jane Doe", "jane@example.com", null);

    private static Polygon validBoundary() {
        return GeometryFixtures.saoPauloParcelA();
    }

    @Test
    void createAssignsGeneratedIdAndAvailableStatus() {
        LandParcel parcel = LandParcel.create("Riverside lot", "Flat terrain", PRICE, CONTACT, validBoundary());

        assertThat(parcel.getId()).isNotNull();
        assertThat(parcel.getStatus()).isEqualTo(ParcelStatus.AVAILABLE);
        assertThat(parcel.getTitle()).isEqualTo("Riverside lot");
        assertThat(parcel.getCreatedAt()).isEqualTo(parcel.getUpdatedAt());
        assertThat(parcel.getVersion()).isZero();
    }

    @Test
    void trimsTitle() {
        LandParcel parcel = LandParcel.create("  Riverside lot  ", null, PRICE, CONTACT, validBoundary());

        assertThat(parcel.getTitle()).isEqualTo("Riverside lot");
    }

    @Test
    void descriptionIsOptional() {
        LandParcel parcel = LandParcel.create("Riverside lot", null, PRICE, CONTACT, validBoundary());

        assertThat(parcel.getDescription()).isNull();
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> LandParcel.create("   ", null, PRICE, CONTACT, validBoundary()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void rejectsTitleLongerThan120Characters() {
        String tooLong = "x".repeat(121);

        assertThatThrownBy(() -> LandParcel.create(tooLong, null, PRICE, CONTACT, validBoundary()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsTitleAtExactly120Characters() {
        String maxLength = "x".repeat(120);

        LandParcel parcel = LandParcel.create(maxLength, null, PRICE, CONTACT, validBoundary());

        assertThat(parcel.getTitle()).hasSize(120);
    }

    @Test
    void rejectsDescriptionLongerThan2000Characters() {
        String tooLong = "x".repeat(2001);

        assertThatThrownBy(() -> LandParcel.create("Title", tooLong, PRICE, CONTACT, validBoundary()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description");
    }

    @Test
    void rejectsBoundaryWithWrongSrid() {
        Polygon wrongSrid = GeometryFixtures.saoPauloParcelA();
        wrongSrid.setSRID(3857);

        assertThatThrownBy(() -> LandParcel.create("Title", null, PRICE, CONTACT, wrongSrid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4326");
    }

    @Test
    void rejectsNullTitle() {
        assertThatThrownBy(() -> LandParcel.create(null, null, PRICE, CONTACT, validBoundary()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullPrice() {
        assertThatThrownBy(() -> LandParcel.create("Title", null, null, CONTACT, validBoundary()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullContact() {
        assertThatThrownBy(() -> LandParcel.create("Title", null, PRICE, null, validBoundary()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullBoundary() {
        assertThatThrownBy(() -> LandParcel.create("Title", null, PRICE, CONTACT, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void markReservedTransitionsFromAvailable() {
        LandParcel parcel = LandParcel.create("Title", null, PRICE, CONTACT, validBoundary());

        parcel.markReserved();

        assertThat(parcel.getStatus()).isEqualTo(ParcelStatus.RESERVED);
    }

    @Test
    void markReservedFailsWhenNotAvailable() {
        LandParcel parcel = LandParcel.create("Title", null, PRICE, CONTACT, validBoundary());
        parcel.markReserved();

        assertThatThrownBy(parcel::markReserved)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AVAILABLE");
    }

    @Test
    void markSoldWorksFromAnyState() {
        LandParcel parcel = LandParcel.create("Title", null, PRICE, CONTACT, validBoundary());

        parcel.markSold();

        assertThat(parcel.getStatus()).isEqualTo(ParcelStatus.SOLD);
    }

    @Test
    void reconstituteBypassesFactoryValidation() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        LandParcel parcel = LandParcel.reconstitute(
                id, "Title", "Desc", PRICE, CONTACT, ParcelStatus.SOLD, validBoundary(), now, now, 5L);

        assertThat(parcel.getId()).isEqualTo(id);
        assertThat(parcel.getStatus()).isEqualTo(ParcelStatus.SOLD);
        assertThat(parcel.getVersion()).isEqualTo(5L);
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        LandParcel a = LandParcel.reconstitute(id, "A", null, PRICE, CONTACT, ParcelStatus.AVAILABLE, validBoundary(), now, now, 0);
        LandParcel b = LandParcel.reconstitute(id, "B", "different", PRICE, CONTACT, ParcelStatus.SOLD, validBoundary(), now, now, 3);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void differentIdsAreNeverEqual() {
        LandParcel a = LandParcel.create("Title", null, PRICE, CONTACT, validBoundary());
        LandParcel b = LandParcel.create("Title", null, PRICE, CONTACT, validBoundary());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toStringDoesNotThrow() {
        LandParcel parcel = LandParcel.create("Title", null, PRICE, CONTACT, validBoundary());

        assertThat(parcel.toString()).contains("Title").contains("AVAILABLE");
    }
}
