package com.terramap.adapter.in.web;

import com.terramap.application.exception.OverlappingParcelException;
import com.terramap.application.exception.ParcelNotFoundException;
import com.terramap.domain.service.GeometryValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.*;

/**
 * Centralized exception handler producing RFC 7807 Problem Details responses.
 * Stack traces are never exposed to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OverlappingParcelException.class)
    public ResponseEntity<Map<String, Object>> handleOverlappingParcel(OverlappingParcelException ex, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://terramap.dev/errors/overlapping-parcel");
        body.put("title", "Overlapping parcel");
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("detail", ex.getMessage());
        body.put("instance", request.getRequestURI());
        body.put("conflictingParcelIds", ex.getConflictingParcelIds());
        body.put("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(GeometryValidationException.class)
    public ResponseEntity<Map<String, Object>> handleGeometryValidation(GeometryValidationException ex, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://terramap.dev/errors/invalid-geometry");
        body.put("title", "Invalid geometry");
        body.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        body.put("detail", ex.getMessage());
        body.put("instance", request.getRequestURI());
        body.put("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(ParcelNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ParcelNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://terramap.dev/errors/not-found");
        body.put("title", "Resource Not Found");
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("detail", ex.getMessage());
        body.put("instance", request.getRequestURI());
        body.put("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://terramap.dev/errors/validation-failed");
        body.put("title", "Validation Failed");
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("detail", "One or more request parameters failed validation");
        body.put("errors", errors);
        body.put("instance", request.getRequestURI());
        body.put("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://terramap.dev/errors/bad-request");
        body.put("title", "Bad Request");
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("detail", ex.getMessage());
        body.put("instance", request.getRequestURI());
        body.put("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://terramap.dev/errors/internal-server-error");
        body.put("title", "Internal Server Error");
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("detail", "An unexpected error occurred. Please try again later.");
        body.put("instance", request.getRequestURI());
        body.put("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
