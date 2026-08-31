package com.terramap.adapter.out.persistence;

import com.terramap.TestcontainersConfiguration;
import com.terramap.adapter.out.persistence.entity.LandParcelEntity;
import com.terramap.domain.model.*;
import com.terramap.support.GeometryFixtures;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies spatial behaviour against a REAL PostGIS instance (never H2 -- see project guide,
 * section "Erros que reprovam projetos como este", item 4: H2 has no ST_Relate/ST_DWithin/GiST).
 *
 * <p>Covers both the application-level overlap check ({@link LandParcelJpaRepository#findOverlappingIds})
 * and the database-level trigger from migration V3, which is the last line of defence against
 * race conditions between two concurrent inserts.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, LandParcelPersistenceAdapter.class})
class LandParcelPersistenceAdapterIT {

    @Autowired
    private LandParcelJpaRepository jpaRepository;

    @Autowired
    private LandParcelPersistenceAdapter adapter;

    private static final WKTWriter WKT_WRITER = new WKTWriter();
    private static final Money PRICE = new Money(new BigDecimal("250000.00"), "BRL");
    private static final ContactInfo CONTACT = new ContactInfo("Jane Doe", "jane@example.com", null);

    private static String wkt(Polygon polygon) {
        return "SRID=4326;" + WKT_WRITER.write(polygon);
    }

    private LandParcelEntity persistEntity(Polygon boundary) {
        LandParcelEntity entity = new LandParcelEntity();
        entity.setId(UUID.randomUUID());
        entity.setTitle("Existing parcel");
        entity.setTotalPrice(new BigDecimal("100000.00"));
        entity.setCurrency("BRL");
        entity.setContactName("Existing Owner");
        entity.setContactEmail("owner@example.com");
        entity.setStatus("AVAILABLE");
        entity.setBoundary(boundary);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return jpaRepository.saveAndFlush(entity);
    }

    // ── Overlap detection (ST_Relate 'T********') ───────────────────────────

    @Test
    void detectsPartialOverlapAgainstRealPostgis() {
        persistEntity(GeometryFixtures.saoPauloParcelA());

        List<String> overlaps = jpaRepository.findOverlappingIds(wkt(GeometryFixtures.saoPauloParcelOverlappingA()));

        assertThat(overlaps).hasSize(1);
    }

    @Test
    void allowsAdjacentParcelsSharingOnlyABorder() {
        persistEntity(GeometryFixtures.saoPauloParcelA());

        List<String> overlaps = jpaRepository.findOverlappingIds(wkt(GeometryFixtures.saoPauloParcelAdjacentToA()));

        assertThat(overlaps).isEmpty();
    }

    @Test
    void allowsParcelsTouchingAtASinglePoint() {
        persistEntity(GeometryFixtures.saoPauloParcelA());

        List<String> overlaps = jpaRepository.findOverlappingIds(wkt(GeometryFixtures.saoPauloParcelTouchingACorner()));

        assertThat(overlaps).isEmpty();
    }

    @Test
    void detectsContainmentInBothDirections() {
        LandParcelEntity big = persistEntity(GeometryFixtures.saoPauloParcelA());

        List<String> overlapsWhenCandidateIsSmaller =
                jpaRepository.findOverlappingIds(wkt(GeometryFixtures.saoPauloParcelContainedInA()));
        assertThat(overlapsWhenCandidateIsSmaller).containsExactly(big.getId().toString());
    }

    @Test
    void detectsIdenticalGeometriesAsOverlap() {
        persistEntity(GeometryFixtures.saoPauloParcelA());

        List<String> overlaps = jpaRepository.findOverlappingIds(wkt(GeometryFixtures.saoPauloParcelIdenticalToA()));

        assertThat(overlaps).hasSize(1);
    }

    @Test
    void allowsCompletelyDisjointParcels() {
        persistEntity(GeometryFixtures.saoPauloParcelA());

        List<String> overlaps = jpaRepository.findOverlappingIds(wkt(GeometryFixtures.rioDeJaneiroParcel()));

        assertThat(overlaps).isEmpty();
    }

    // ── Database trigger (defence in depth against race conditions) ────────

    @Test
    void databaseTriggerRejectsOverlappingInsertEvenWithoutApplicationCheck() {
        persistEntity(GeometryFixtures.saoPauloParcelA());

        LandParcelEntity overlappingEntity = new LandParcelEntity();
        overlappingEntity.setId(UUID.randomUUID());
        overlappingEntity.setTitle("Fraudulent duplicate");
        overlappingEntity.setTotalPrice(new BigDecimal("1.00"));
        overlappingEntity.setCurrency("BRL");
        overlappingEntity.setContactName("Someone");
        overlappingEntity.setContactEmail("someone@example.com");
        overlappingEntity.setStatus("AVAILABLE");
        overlappingEntity.setBoundary(GeometryFixtures.saoPauloParcelOverlappingA());
        overlappingEntity.setCreatedAt(Instant.now());
        overlappingEntity.setUpdatedAt(Instant.now());

        // Bypasses the application-level check in RegisterLandParcelService on purpose,
        // simulating a race where two requests both passed validation before either committed.
        // The root cause message is asserted (rather than the top-level wrapper message,
        // whose exact wording is a Hibernate/driver implementation detail) because that is
        // where the trigger's RAISE EXCEPTION text ends up.
        assertThatThrownBy(() -> jpaRepository.saveAndFlush(overlappingEntity))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertThat(rootCauseMessage(ex)).contains("OVERLAPPING_PARCEL"));
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return String.valueOf(current.getMessage());
    }

    // ── Radius search (ST_DWithin on geography cast) ────────────────────────

    @Test
    void findsParcelsWithinSearchRadius() {
        persistEntity(GeometryFixtures.saoPauloParcelA());

        // Center of the search circle sits inside Parcel A's own bounding area
        List<LandParcelEntity> results =
                jpaRepository.findWithinRadius(-46.630, -23.550, 500, 10, 0);

        assertThat(results).hasSize(1);
    }

    @Test
    void excludesParcelsOutsideSearchRadius() {
        persistEntity(GeometryFixtures.rioDeJaneiroParcel());

        // Searching in Sao Paulo; Rio de Janeiro is ~350km away, far beyond a 1km radius
        List<LandParcelEntity> results =
                jpaRepository.findWithinRadius(-46.630, -23.550, 1000, 10, 0);

        assertThat(results).isEmpty();
    }

    @Test
    void ordersResultsByDistanceFromCenter() {
        LandParcelEntity near = persistEntity(GeometryFixtures.saoPauloParcelA());
        LandParcelEntity far = persistEntity(GeometryFixtures.saoPauloParcelAdjacentToA());

        // Center closer to Parcel A than to its eastern neighbour
        List<LandParcelEntity> results =
                jpaRepository.findWithinRadius(-46.6345, -23.550, 5000, 10, 0);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results.get(0).getId()).isEqualTo(near.getId());
    }

    @Test
    void respectsPageSizeAndOffset() {
        persistEntity(GeometryFixtures.saoPauloParcelA());
        persistEntity(GeometryFixtures.saoPauloParcelAdjacentToA());

        List<LandParcelEntity> firstPage =
                jpaRepository.findWithinRadius(-46.630, -23.550, 5000, 1, 0);
        List<LandParcelEntity> secondPage =
                jpaRepository.findWithinRadius(-46.630, -23.550, 5000, 1, 1);

        assertThat(firstPage).hasSize(1);
        assertThat(secondPage).hasSize(1);
        assertThat(firstPage.get(0).getId()).isNotEqualTo(secondPage.get(0).getId());
    }

    // ── Adapter round trip (domain <-> entity mapping, incl. SRID) ──────────

    @Test
    void adapterSaveAndFindByIdRoundTripPreservesDomainData() {
        LandParcel parcel = LandParcel.create(
                "Riverside lot", "Flat terrain", PRICE, CONTACT, GeometryFixtures.saoPauloParcelA());

        LandParcel saved = adapter.save(parcel);
        Optional<LandParcel> reloaded = adapter.findById(saved.getId());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getId()).isEqualTo(parcel.getId());
        assertThat(reloaded.get().getTitle()).isEqualTo("Riverside lot");
        assertThat(reloaded.get().getTotalPrice()).isEqualTo(PRICE);
        assertThat(reloaded.get().getContact().getEmail()).isEqualTo("jane@example.com");
        // The boundary round-trips through PostGIS and must keep its SRID
        assertThat(reloaded.get().getBoundary().getSRID()).isEqualTo(4326);
    }

    @Test
    void adapterFindByIdReturnsEmptyWhenNotFound() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void adapterFindOverlappingIdsDelegatesToRepository() {
        LandParcel existing = LandParcel.create(
                "Existing", null, PRICE, CONTACT, GeometryFixtures.saoPauloParcelA());
        adapter.save(existing);

        List<UUID> overlaps = adapter.findOverlappingIds(GeometryFixtures.saoPauloParcelOverlappingA());

        assertThat(overlaps).containsExactly(existing.getId());
    }
}