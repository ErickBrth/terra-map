package com.terramap.application.usecase;

import com.terramap.application.exception.ParcelNotFoundException;
import com.terramap.application.port.out.LandParcelRepositoryPort;
import com.terramap.domain.model.ContactInfo;
import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.Money;
import com.terramap.domain.model.ParcelStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateParcelStatusServiceTest {

    @Mock
    private LandParcelRepositoryPort repository;

    private LandParcel newAvailableParcel() {
        return LandParcel.create("Title", null,
                new Money(BigDecimal.TEN, "BRL"),
                new ContactInfo("Jane Doe", "jane@example.com", null),
                GeometryFixtures.saoPauloParcelA());
    }

    @Test
    void reserveTransitionsAnAvailableParcelAndPersistsIt() {
        LandParcel parcel = newAvailableParcel();
        when(repository.findById(parcel.getId())).thenReturn(Optional.of(parcel));
        when(repository.save(any(LandParcel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateParcelStatusService service = new UpdateParcelStatusService(repository);
        LandParcel result = service.reserve(parcel.getId());

        assertThat(result.getStatus()).isEqualTo(ParcelStatus.RESERVED);
        verify(repository).save(parcel);
    }

    @Test
    void reserveFailsWhenParcelIsNotAvailable() {
        LandParcel parcel = newAvailableParcel();
        parcel.markSold(); // no longer AVAILABLE
        when(repository.findById(parcel.getId())).thenReturn(Optional.of(parcel));

        UpdateParcelStatusService service = new UpdateParcelStatusService(repository);

        assertThatThrownBy(() -> service.reserve(parcel.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AVAILABLE");
    }

    @Test
    void reserveThrowsNotFoundForMissingParcel() {
        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        UpdateParcelStatusService service = new UpdateParcelStatusService(repository);

        assertThatThrownBy(() -> service.reserve(missingId))
                .isInstanceOf(ParcelNotFoundException.class);
    }

    @Test
    void markSoldWorksFromAvailable() {
        LandParcel parcel = newAvailableParcel();
        when(repository.findById(parcel.getId())).thenReturn(Optional.of(parcel));
        when(repository.save(any(LandParcel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateParcelStatusService service = new UpdateParcelStatusService(repository);
        LandParcel result = service.markSold(parcel.getId());

        assertThat(result.getStatus()).isEqualTo(ParcelStatus.SOLD);
    }

    @Test
    void markSoldThrowsNotFoundForMissingParcel() {
        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        UpdateParcelStatusService service = new UpdateParcelStatusService(repository);

        assertThatThrownBy(() -> service.markSold(missingId))
                .isInstanceOf(ParcelNotFoundException.class);
    }
}
