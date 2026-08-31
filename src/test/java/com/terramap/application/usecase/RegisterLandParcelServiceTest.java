package com.terramap.application.usecase;

import com.terramap.application.exception.OverlappingParcelException;
import com.terramap.application.port.in.RegisterLandParcelUseCase;
import com.terramap.application.port.out.LandParcelRepositoryPort;
import com.terramap.domain.model.ContactInfo;
import com.terramap.domain.model.LandParcel;
import com.terramap.domain.model.Money;
import com.terramap.domain.service.GeometryValidationException;
import com.terramap.domain.service.GeometryValidator;
import com.terramap.support.GeometryFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Polygon;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterLandParcelServiceTest {

    @Mock
    private GeometryValidator geometryValidator;

    @Mock
    private LandParcelRepositoryPort repository;

    private RegisterLandParcelService service;

    private static final Money PRICE = new Money(new BigDecimal("250000.00"), "BRL");
    private static final ContactInfo CONTACT = new ContactInfo("Jane Doe", "jane@example.com", null);

    @BeforeEach
    void setUp() {
        service = new RegisterLandParcelService(geometryValidator, repository);
    }

    private RegisterLandParcelUseCase.Command commandWithBoundary(Polygon boundary) {
        return new RegisterLandParcelUseCase.Command("Riverside lot", "Nice view", PRICE, CONTACT, boundary);
    }

    @Test
    void registersParcelWhenNoOverlapExists() {
        Polygon boundary = GeometryFixtures.saoPauloParcelA();
        when(repository.findOverlappingIds(boundary)).thenReturn(List.of());
        when(repository.save(any(LandParcel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LandParcel result = service.register(commandWithBoundary(boundary));

        assertThat(result.getTitle()).isEqualTo("Riverside lot");
        assertThat(result.getStatus().name()).isEqualTo("AVAILABLE");
        verify(repository).save(any(LandParcel.class));
    }

    @Test
    void validatesGeometryBeforeCheckingOverlap() {
        Polygon boundary = GeometryFixtures.saoPauloParcelA();
        when(repository.findOverlappingIds(boundary)).thenReturn(List.of());
        when(repository.save(any(LandParcel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(commandWithBoundary(boundary));

        InOrder order = inOrder(geometryValidator, repository);
        order.verify(geometryValidator).validate(boundary);
        order.verify(repository).findOverlappingIds(boundary);
        order.verify(repository).save(any(LandParcel.class));
    }

    @Test
    void propagatesGeometryValidationExceptionAndNeverQueriesRepository() {
        Polygon invalidBoundary = GeometryFixtures.selfIntersectingBowtie();
        doThrow(new GeometryValidationException("boundary self-intersects"))
                .when(geometryValidator).validate(invalidBoundary);

        assertThatThrownBy(() -> service.register(commandWithBoundary(invalidBoundary)))
                .isInstanceOf(GeometryValidationException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void throwsOverlappingParcelExceptionWhenRepositoryFindsConflicts() {
        Polygon boundary = GeometryFixtures.saoPauloParcelOverlappingA();
        UUID conflictingId = UUID.randomUUID();
        when(repository.findOverlappingIds(boundary)).thenReturn(List.of(conflictingId));

        assertThatThrownBy(() -> service.register(commandWithBoundary(boundary)))
                .isInstanceOf(OverlappingParcelException.class)
                .satisfies(ex -> assertThat(((OverlappingParcelException) ex).getConflictingParcelIds())
                        .containsExactly(conflictingId));

        verify(repository, never()).save(any());
    }

    @Test
    void reportsAllConflictingIdsWhenMultipleParcelsOverlap() {
        Polygon boundary = GeometryFixtures.saoPauloParcelOverlappingA();
        List<UUID> conflicts = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(repository.findOverlappingIds(boundary)).thenReturn(conflicts);

        assertThatThrownBy(() -> service.register(commandWithBoundary(boundary)))
                .isInstanceOfSatisfying(OverlappingParcelException.class,
                        ex -> assertThat(ex.getConflictingParcelIds()).hasSize(2));
    }
}
