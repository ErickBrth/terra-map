package com.terramap.domain.model;

import org.locationtech.jts.geom.Polygon;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a land parcel listing.
 *
 * <p>Construction is only possible through the {@link #create} factory method,
 * which enforces all business invariants. The domain model has no Spring or
 * JPA dependency — it can be tested in isolation without starting any container.
 *
 * <p>Geometry is stored in <strong>EPSG:4326</strong> (WGS84, lon/lat degrees),
 * as required by GeoJSON RFC 7946 and the PostGIS column definition
 * {@code geometry(Polygon, 4326)}.
 */
public class LandParcel {

    /** Maximum number of polygon vertices, aligned with {@code terramap.geometry.max-vertices}. */
    public static final int MAX_VERTICES = 1000;

    private final UUID id;
    private String title;
    private String description;
    private Money totalPrice;
    private ContactInfo contact;
    private ParcelStatus status;
    private final Polygon boundary;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    private LandParcel(UUID id, String title, String description,
                       Money totalPrice, ContactInfo contact,
                       ParcelStatus status, Polygon boundary,
                       Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.totalPrice = totalPrice;
        this.contact = contact;
        this.status = status;
        this.boundary = boundary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /**
     * Factory method — the only valid way to create a new parcel.
     *
     * @param title       short human-readable label (max 120 chars)
     * @param description optional long description (max 2000 chars)
     * @param totalPrice  asking price, must be positive
     * @param contact     advertiser contact, must have name and valid email
     * @param boundary    polygon in EPSG:4326; must have SRID 4326
     */
    public static LandParcel create(String title, String description,
                                    Money totalPrice, ContactInfo contact,
                                    Polygon boundary) {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(totalPrice, "totalPrice must not be null");
        Objects.requireNonNull(contact, "contact must not be null");
        Objects.requireNonNull(boundary, "boundary must not be null");

        if (title.isBlank() || title.length() > 120) {
            throw new IllegalArgumentException(
                    "title must be 1–120 characters, got " + title.length());
        }
        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException(
                    "description must not exceed 2000 characters");
        }
        if (boundary.getSRID() != 4326) {
            throw new IllegalArgumentException(
                    "boundary must be in EPSG:4326, got SRID " + boundary.getSRID());
        }

        Instant now = Instant.now();
        return new LandParcel(
                UUID.randomUUID(), title.strip(), description,
                totalPrice, contact, ParcelStatus.AVAILABLE,
                boundary, now, now, null);
    }

    /**
     * Reconstitutes an existing parcel from persistence.
     * Bypasses factory-level validations that have already been enforced at write time.
     */
    public static LandParcel reconstitute(UUID id, String title, String description,
                                          Money totalPrice, ContactInfo contact,
                                          ParcelStatus status, Polygon boundary,
                                          Instant createdAt, Instant updatedAt, Long version) {
        return new LandParcel(id, title, description, totalPrice, contact,
                status, boundary, createdAt, updatedAt, version);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Money getTotalPrice() { return totalPrice; }
    public ContactInfo getContact() { return contact; }
    public ParcelStatus getStatus() { return status; }
    public Polygon getBoundary() { return boundary; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    // ── Business operations ───────────────────────────────────────────────────

    public void markReserved() {
        if (this.status != ParcelStatus.AVAILABLE) {
            throw new IllegalStateException("Only AVAILABLE parcels can be reserved");
        }
        this.status = ParcelStatus.RESERVED;
        this.updatedAt = Instant.now();
    }

    public void markSold() {
        this.status = ParcelStatus.SOLD;
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LandParcel other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LandParcel{id=" + id + ", title='" + title + "', status=" + status + "}";
    }
}
