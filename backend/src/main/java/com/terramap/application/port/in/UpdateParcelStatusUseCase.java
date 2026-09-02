package com.terramap.application.port.in;

import com.terramap.domain.model.LandParcel;

import java.util.UUID;

/**
 * Input port exposing the parcel status transitions already defined on the
 * domain ({@link LandParcel#markReserved()}, {@link LandParcel#markSold()}).
 * One port for both, since they're the same kind of operation (load, mutate,
 * save) and don't warrant two near-identical use case interfaces.
 */
public interface UpdateParcelStatusUseCase {

    /**
     * @throws com.terramap.application.exception.ParcelNotFoundException if no parcel with the given ID exists
     * @throws IllegalStateException if the parcel is not currently AVAILABLE
     */
    LandParcel reserve(UUID id);

    /**
     * @throws com.terramap.application.exception.ParcelNotFoundException if no parcel with the given ID exists
     */
    LandParcel markSold(UUID id);
}
