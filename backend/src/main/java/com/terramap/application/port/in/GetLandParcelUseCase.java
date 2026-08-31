package com.terramap.application.port.in;

import com.terramap.domain.model.LandParcel;

import java.util.UUID;

/**
 * Input port for retrieving a single parcel by ID.
 */
public interface GetLandParcelUseCase {

    /**
     * @throws com.terramap.application.exception.ParcelNotFoundException if no parcel with the given ID exists
     */
    LandParcel getById(UUID id);
}
