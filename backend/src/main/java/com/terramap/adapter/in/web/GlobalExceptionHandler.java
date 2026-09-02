package com.terramap.adapter.in.web;

import com.terramap.application.exception.OverlappingParcelException;
import com.terramap.application.exception.ParcelNotFoundException;
import com.terramap.domain.service.GeometryValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handler producing RFC 7807 Problem Details responses.
 * Stack traces are never exposed to the client — the real exception is logged
 * server-side and the client gets a generic message instead.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_TYPE_BASE = "https://terramap.dev/errors/";

    /** Builds the common RFC 7807 fields every handler below returns. */
    private static Map<String, Object> problemDetail(HttpStatus status, String typeSlug, String title,
                                                      String detail, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", ERROR_TYPE_BASE + typeSlug);
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());
        body.put("timestamp", Instant.now());
        return body;
    }

    @ExceptionHandler(OverlappingParcelException.class)
    public ResponseEntity<Map<String, Object>> handleOverlappingParcel(OverlappingParcelException ex, HttpServletRequest request) {
        Map<String, Object> body = problemDetail(HttpStatus.CONFLICT, "overlapping-parcel", "Overlapping parcel", ex.getMessage(), request);
        body.put("conflictingParcelIds", ex.getConflictingParcelIds());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * The database trigger (migration V3) is the last line of defence against two
     * concurrent registrations racing past the application-level overlap check at
     * the same time. When it fires, Spring translates the underlying constraint
     * violation into this exception — mapped to the same 409 the application-level
     * check returns, so the client sees one consistent contract for "this
     * overlaps," regardless of which layer actually caught it.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation, likely a concurrent overlap race caught by the DB trigger: {}", ex.getMessage());
        Map<String, Object> body = problemDetail(HttpStatus.CONFLICT, "overlapping-parcel", "Overlapping parcel",
                "The boundary overlaps an existing parcel.", request);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(GeometryValidationException.class)
    public ResponseEntity<Map<String, Object>> handleGeometryValidation(GeometryValidationException ex, HttpServletRequest request) {
        Map<String, Object> body = problemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-geometry", "Invalid geometry", ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(ParcelNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ParcelNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> body = problemDetail(HttpStatus.NOT_FOUND, "not-found", "Resource Not Found", ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = problemDetail(HttpStatus.BAD_REQUEST, "validation-failed", "Validation Failed",
                "One or more request parameters failed validation", request);
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        Map<String, Object> body = problemDetail(HttpStatus.BAD_REQUEST, "bad-request", "Bad Request", ex.getMessage(), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        Map<String, Object> body = problemDetail(HttpStatus.BAD_REQUEST, "bad-request", "Bad Request",
                "Invalid parameter value for: " + ex.getName(), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error handling {} {}", request.getMethod(), request.getRequestURI(), ex);
        Map<String, Object> body = problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "internal-server-error",
                "Internal Server Error", "An unexpected error occurred. Please try again later.", request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
