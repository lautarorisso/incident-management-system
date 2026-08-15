package com.lautarorisso.user_service.exception;

/**
 * Thrown when a requested resource (e.g. a user or team) cannot be found.
 * <p>
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
