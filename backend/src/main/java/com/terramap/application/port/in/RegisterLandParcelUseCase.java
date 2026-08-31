package com.terramap.application.port.in;

import com.terramap.domain.model.ContactInfo;
import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.Money;
import org.locationtech.jts.geom.Polygon;

/**
 * Input port for registering a new land parcel.
 */
public interface RegisterLandParcelUseCase {

    /**
     * Validates, checks for overlap, and persists a new parcel.
     *
     * @return the persisted parcel (with generated UUID and timestamps)
     * @throws com.terramap.domain.service.GeometryValidationException if the boundary is invalid
     * @throws com.terramap.application.exception.OverlappingParcelException if the boundary overlaps an existing parcel
     */
    LandParcel register(Command command);

    record Command(
            String title,
            String description,
            Money totalPrice,
            ContactInfo contact,
            Polygon boundary
    ) {}
}
