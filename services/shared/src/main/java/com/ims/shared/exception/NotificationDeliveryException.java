package com.ims.shared.exception;

/**
 * Thrown when an external notification delivery (e.g. email) fails.
 * <p>
 * Mapped to HTTP 502 by {@link BaseGlobalExceptionHandler}.
 */
public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
