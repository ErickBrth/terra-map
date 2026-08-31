package com.terramap.application.exception;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when a new parcel boundary overlaps with one or more existing parcels.
 * Maps to HTTP 409 Conflict with a body containing {@code conflictingParcelIds}.
 */
public class OverlappingParcelException extends RuntimeException {

    private final List<UUID> conflictingParcelIds;

    public OverlappingParcelException(List<UUID> conflictingParcelIds) {
        super("The boundary overlaps " + conflictingParcelIds.size() + " existing parcel(s): " + conflictingParcelIds);
        this.conflictingParcelIds = List.copyOf(conflictingParcelIds);
    }

    public List<UUID> getConflictingParcelIds() {
        return conflictingParcelIds;
    }
}
