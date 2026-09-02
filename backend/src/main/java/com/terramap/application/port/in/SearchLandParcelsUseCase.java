package com.terramap.application.port.in;

import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.SearchArea;

import java.util.List;

/**
 * Input port for searching land parcels within a circular area.
 */
public interface SearchLandParcelsUseCase {

    /**
     * Returns all parcels whose boundary intersects the search circle.
     * Results are ordered by distance from the center (closest first).
     */
    List<LandParcel> search(Query query);

    record Query(
            SearchArea searchArea,
            int page,
            int size
    ) {}
}
