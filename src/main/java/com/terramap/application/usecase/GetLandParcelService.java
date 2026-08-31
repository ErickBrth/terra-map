package com.terramap.application.usecase;

import com.terramap.application.exception.ParcelNotFoundException;
import com.terramap.application.port.in.GetLandParcelUseCase;
import com.terramap.application.port.out.LandParcelRepositoryPort;
import com.terramap.domain.model.LandParcel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Retrieves a single land parcel by UUID.
 */
@Service
@Transactional(readOnly = true)
public class GetLandParcelService implements GetLandParcelUseCase {

    private final LandParcelRepositoryPort repository;

    public GetLandParcelService(LandParcelRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public LandParcel getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ParcelNotFoundException(id));
    }
}
