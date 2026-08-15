package com.lautarorisso.incident_service.exception;

/**
 * Thrown when a requested Incident (or related resource) cannot be found.
 * <p>
 * Mapped to HTTP 404 by {@link com.lautarorisso.incident_service.exception.GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
