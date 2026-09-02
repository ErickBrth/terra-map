package com.terramap.application.usecase;

import com.terramap.application.port.in.SearchLandParcelsUseCase;
import com.terramap.application.port.out.LandParcelRepositoryPort;
import com.terramap.domain.model.LandParcel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates parcel search within a circular geographic area, with optional
 * price and status filters applied at the database level.
 */
@Service
@Transactional(readOnly = true)
public class SearchLandParcelsService implements SearchLandParcelsUseCase {

    private final LandParcelRepositoryPort repository;

    public SearchLandParcelsService(LandParcelRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<LandParcel> search(Query query) {
        return repository.findWithinRadius(
                query.searchArea(), query.maxPrice(), query.status(), query.page(), query.size());
    }
}
