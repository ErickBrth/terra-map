package com.terramap.application.usecase;

import com.terramap.application.exception.ParcelNotFoundException;
import com.terramap.application.port.out.LandParcelRepositoryPort;
import com.terramap.domain.model.ContactInfo;
import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.Money;
import com.terramap.support.GeometryFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLandParcelServiceTest {

    @Mock
    private LandParcelRepositoryPort repository;

    @Test
    void returnsParcelWhenFound() {
        LandParcel parcel = LandParcel.create("Title", null,
                new Money(BigDecimal.TEN, "BRL"),
                new ContactInfo("Jane Doe", "jane@example.com", null),
                GeometryFixtures.saoPauloParcelA());
        when(repository.findById(parcel.getId())).thenReturn(Optional.of(parcel));

        GetLandParcelService service = new GetLandParcelService(repository);

        assertThat(service.getById(parcel.getId())).isEqualTo(parcel);
    }

    @Test
    void throwsNotFoundWhenAbsent() {
        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        GetLandParcelService service = new GetLandParcelService(repository);

        assertThatThrownBy(() -> service.getById(missingId))
                .isInstanceOf(ParcelNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }
}
