package com.terramap.domain.service;

/**
 * Thrown when a submitted polygon fails geometric validation.
 * Maps to HTTP 422 Unprocessable Entity in the web adapter.
 */
public class GeometryValidationException extends RuntimeException {

    public GeometryValidationException(String message) {
        super(message);
    }
}
