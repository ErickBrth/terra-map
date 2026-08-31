package com.terramap.application.exception;

import java.util.UUID;

/**
 * Thrown when a requested parcel ID does not exist.
 * Maps to HTTP 404 Not Found.
 */
public class ParcelNotFoundException extends RuntimeException {

    public ParcelNotFoundException(UUID id) {
        super("Land parcel not found: " + id);
    }
}
