package com.terramap.application.usecase;

import com.terramap.application.exception.OverlappingParcelException;
import com.terramap.application.port.in.RegisterLandParcelUseCase;
import com.terramap.application.port.out.LandParcelRepositoryPort;
import com.terramap.domain.model.LandParcel;
import com.terramap.domain.service.GeometryValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the registration of a new land parcel.
 *
 * <p>The overlap rule (DE-9IM interior-interior intersection — see
 * {@link LandParcelRepositoryPort#findOverlappingIds}) is enforced here for a
 * clear 409 response, AND again as a database trigger. The dual check
 * prevents race conditions between two concurrent registrations.
 */
@Service
@Transactional
public class RegisterLandParcelService implements RegisterLandParcelUseCase {

    private final GeometryValidator geometryValidator;
    private final LandParcelRepositoryPort repository;

    public RegisterLandParcelService(GeometryValidator geometryValidator,
                                     LandParcelRepositoryPort repository) {
        this.geometryValidator = geometryValidator;
        this.repository = repository;
    }

    @Override
    public LandParcel register(Command command) {
        geometryValidator.validate(command.boundary());

        List<UUID> overlappingIds = repository.findOverlappingIds(command.boundary());
        if (!overlappingIds.isEmpty()) {
            throw new OverlappingParcelException(overlappingIds);
        }

        LandParcel parcel = LandParcel.create(
                command.title(),
                command.description(),
                command.totalPrice(),
                command.contact(),
                command.boundary()
        );

        return repository.save(parcel);
    }
}
