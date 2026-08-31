package com.terramap.application.usecase;

import com.terramap.application.exception.OverlappingParcelException;
import com.terramap.application.port.in.RegisterLandParcelUseCase;
import com.terramap.application.port.out.LandParcelRepositoryPort;
import com.terramap.domain.model.LandParcel;
import com.terramap.domain.service.GeometryValidator;
import com.terramap.domain.service.JtsOverlapPolicy;
import com.terramap.domain.service.OverlapPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the registration of a new land parcel.
 *
 * <p>Execution flow:
 * <ol>
 *   <li>Validate geometry (SRID, validity, vertex count)</li>
 *   <li>Check for interior-interior overlap with existing parcels (JTS + DB)</li>
 *   <li>Persist and return the new parcel</li>
 * </ol>
 *
 * <p>The overlap check is performed at the application layer (fast, in-memory,
 * against a pre-filtered set) AND enforced at the database layer via a trigger
 * ({@code trg_land_parcel_no_overlap}). The dual check prevents race conditions.
 */
@Service
@Transactional
public class RegisterLandParcelService implements RegisterLandParcelUseCase {

    private final GeometryValidator geometryValidator;
    private final OverlapPolicy overlapPolicy;
    private final LandParcelRepositoryPort repository;

    public RegisterLandParcelService(GeometryValidator geometryValidator,
                                     LandParcelRepositoryPort repository) {
        this.geometryValidator = geometryValidator;
        this.overlapPolicy = new JtsOverlapPolicy();
        this.repository = repository;
    }

    @Override
    public LandParcel register(Command command) {
        // 1. Geometry validation (throws GeometryValidationException → 422)
        geometryValidator.validate(command.boundary());

        // 2. Overlap check — query only parcels with bounding-box overlap first
        List<UUID> overlappingIds = repository.findOverlappingIds(command.boundary());
        if (!overlappingIds.isEmpty()) {
            throw new OverlappingParcelException(overlappingIds);
        }

        // 3. Create aggregate and persist
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
