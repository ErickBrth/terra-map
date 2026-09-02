package com.terramap.application.usecase;

import com.terramap.application.exception.ParcelNotFoundException;
import com.terramap.application.port.in.UpdateParcelStatusUseCase;
import com.terramap.application.port.out.LandParcelRepositoryPort;
import com.terramap.domain.model.LandParcel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Applies a status transition to an existing parcel. All the business rule
 * (which transitions are legal) lives in {@link LandParcel} itself — this
 * class only loads, delegates, and persists.
 */
@Service
@Transactional
public class UpdateParcelStatusService implements UpdateParcelStatusUseCase {

    private final LandParcelRepositoryPort repository;

    public UpdateParcelStatusService(LandParcelRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public LandParcel reserve(UUID id) {
        LandParcel parcel = findOrThrow(id);
        parcel.markReserved();
        return repository.save(parcel);
    }

    @Override
    public LandParcel markSold(UUID id) {
        LandParcel parcel = findOrThrow(id);
        parcel.markSold();
        return repository.save(parcel);
    }

    private LandParcel findOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ParcelNotFoundException(id));
    }
}
