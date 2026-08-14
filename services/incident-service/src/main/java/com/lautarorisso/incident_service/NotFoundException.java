package com.lautarorisso.incident_service;

/**
 * Thrown when a requested Incident (or related resource) cannot be found.
 * <p>
 * Mapped to HTTP 404 by {@link com.lautarorisso.incident_service.controller.GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
