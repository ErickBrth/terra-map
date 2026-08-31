package com.terramap.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code land_parcel} table.
 *
 * <p>This class has no business logic — it is a pure data-transfer object
 * between the application and the database. All domain concepts live in
 * {@link com.terramap.domain.model.LandParcel}.
 *
 * <p>The {@code boundary} column uses PostGIS type {@code geometry(Polygon,4326)}.
 * Hibernate Spatial 7 reads and writes JTS {@link Polygon} objects natively.
 */
@Entity
@Table(name = "land_parcel")
@Getter
@Setter
public class LandParcelEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(length = 120, nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(name = "contact_name", length = 120, nullable = false)
    private String contactName;

    @Column(name = "contact_email", length = 180, nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(length = 20, nullable = false)
    private String status;

    /**
     * Spatial column — PostGIS geometry(Polygon, 4326).
     * Hibernate Spatial maps this to a JTS {@link Polygon} transparently.
     * The SRID constraint is enforced by the column definition in V2 migration.
     */
    @Column(columnDefinition = "geometry(Polygon,4326)", nullable = false)
    private Polygon boundary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Optimistic locking — prevents lost-update anomalies on concurrent edits.
     *
     * <p>Kept as the wrapper type {@link Long} (never the primitive {@code long}) on
     * purpose: Spring Data's rule for deciding INSERT vs. UPDATE, when a {@code @Version}
     * field is present, is simply "is this field null?". Null means "treat as new row"
     * (INSERT); non-null — even zero — means "this row already exists" (UPDATE).
     * See {@link com.terramap.adapter.out.persistence.LandParcelPersistenceAdapter#toEntity}
     * for the mapping rule that keeps this consistent for brand-new parcels.
     */
    @Version
    private Long version;
}