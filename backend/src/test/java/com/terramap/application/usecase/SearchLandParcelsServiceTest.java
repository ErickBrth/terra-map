package com.terramap.application.usecase;

import com.terramap.application.port.in.SearchLandParcelsUseCase;
import com.terramap.application.port.out.LandParcelRepositoryPort;
import com.terramap.domain.model.*;
import com.terramap.support.GeometryFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchLandParcelsServiceTest {

    @Mock
    private LandParcelRepositoryPort repository;

    @Test
    void delegatesToRepositoryWithSearchAreaPageAndSize() {
        SearchArea area = new SearchArea(GeometryFixtures.createPoint(-46.63, -23.55), 1500);
        LandParcel parcel = LandParcel.create("Title", null,
                new Money(BigDecimal.TEN, "BRL"),
                new ContactInfo("Jane Doe", "jane@example.com", null),
                GeometryFixtures.saoPauloParcelA());
        when(repository.findWithinRadius(area, null, null, 0, 100)).thenReturn(List.of(parcel));

        SearchLandParcelsService service = new SearchLandParcelsService(repository);
        SearchLandParcelsUseCase.Query query = new SearchLandParcelsUseCase.Query(area, null, null, 0, 100);

        List<LandParcel> result = service.search(query);

        assertThat(result).containsExactly(parcel);
        verify(repository).findWithinRadius(area, null, null, 0, 100);
    }

    @Test
    void forwardsMaxPriceAndStatusFiltersToRepository() {
        SearchArea area = new SearchArea(GeometryFixtures.createPoint(-46.63, -23.55), 1500);
        BigDecimal maxPrice = new BigDecimal("500000.00");
        when(repository.findWithinRadius(area, maxPrice, ParcelStatus.AVAILABLE, 0, 100)).thenReturn(List.of());

        SearchLandParcelsService service = new SearchLandParcelsService(repository);
        SearchLandParcelsUseCase.Query query =
                new SearchLandParcelsUseCase.Query(area, maxPrice, ParcelStatus.AVAILABLE, 0, 100);

        service.search(query);

        verify(repository).findWithinRadius(area, maxPrice, ParcelStatus.AVAILABLE, 0, 100);
    }

    @Test
    void returnsEmptyListWhenNothingFound() {
        SearchArea area = new SearchArea(GeometryFixtures.createPoint(0, 0), 500);
        when(repository.findWithinRadius(area, null, null, 0, 50)).thenReturn(List.of());

        SearchLandParcelsService service = new SearchLandParcelsService(repository);
        SearchLandParcelsUseCase.Query query = new SearchLandParcelsUseCase.Query(area, null, null, 0, 50);

        assertThat(service.search(query)).isEmpty();
    }
}
