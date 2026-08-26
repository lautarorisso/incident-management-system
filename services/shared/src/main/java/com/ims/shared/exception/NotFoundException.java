package com.ims.shared.exception;

/**
 * Thrown when a requested resource cannot be found.
 * <p>
 * Mapped to HTTP 404 by {@link BaseGlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
