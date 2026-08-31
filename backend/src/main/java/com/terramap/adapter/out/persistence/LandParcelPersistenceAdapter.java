package com.terramap.adapter.out.persistence;

import com.terramap.adapter.out.persistence.entity.LandParcelEntity;
import com.terramap.application.port.out.LandParcelRepositoryPort;
import com.terramap.domain.model.*;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter translating between the domain model ({@link LandParcel}) and the JPA
 * entity ({@link LandParcelEntity}).
 *
 * <p>All PostGIS-specific details (WKT conversion, geography cast, ST_DWithin) are
 * confined to this class and {@link LandParcelJpaRepository}.
 * The domain and application layers never see JPA annotations or SQL.
 */
@Component
public class LandParcelPersistenceAdapter implements LandParcelRepositoryPort {

    private final LandParcelJpaRepository jpaRepository;
    private final WKTWriter wktWriter = new WKTWriter();

    public LandParcelPersistenceAdapter(LandParcelJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public LandParcel save(LandParcel parcel) {
        LandParcelEntity entity = toEntity(parcel);
        LandParcelEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<LandParcel> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<LandParcel> findWithinRadius(com.terramap.domain.model.SearchArea searchArea, int page, int size) {
        double lon = searchArea.getCenter().getX(); // GeoJSON: [lon, lat] → X = lon
        double lat = searchArea.getCenter().getY();
        int offset = page * size;

        return jpaRepository
                .findWithinRadius(lon, lat, searchArea.getRadiusInMeters(), size, offset)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findOverlappingIds(Polygon candidate) {
        // Pass the polygon as WKT; the query casts it back to geometry with the correct SRID
        String wkt = "SRID=4326;" + wktWriter.write(candidate);
        return jpaRepository.findOverlappingIds(wkt)
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private LandParcelEntity toEntity(LandParcel domain) {
        LandParcelEntity entity = new LandParcelEntity();
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setTotalPrice(domain.getTotalPrice().getAmount());
        entity.setCurrency(domain.getTotalPrice().getCurrency());
        entity.setContactName(domain.getContact().getName());
        entity.setContactEmail(domain.getContact().getEmail());
        entity.setContactPhone(domain.getContact().getPhone());
        entity.setStatus(domain.getStatus().name());
        entity.setBoundary(domain.getBoundary());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        // A brand-new LandParcel reports version == 0, but Hibernate's own unsaved-value
        // check (independent of Spring Data's Persistable) treats a non-null @Version as
        // "this row already exists". Passing null here lets Hibernate initialise the version
        // itself on INSERT. Persistable.isNew() (see LandParcelEntity) is what actually
        // decides insert vs. update; this only avoids a second, conflicting signal.
        entity.setVersion(domain.getVersion() > 0 ? domain.getVersion() : null);
        return entity;
    }

    private LandParcel toDomain(LandParcelEntity entity) {
        return LandParcel.reconstitute(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                new Money(entity.getTotalPrice(), entity.getCurrency()),
                new ContactInfo(entity.getContactName(), entity.getContactEmail(), entity.getContactPhone()),
                ParcelStatus.valueOf(entity.getStatus()),
                entity.getBoundary(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}