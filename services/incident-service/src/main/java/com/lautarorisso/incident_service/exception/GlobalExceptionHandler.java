package com.lautarorisso.incident_service.controller;

import com.lautarorisso.incident_service.NotFoundException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handler for REST endpoints.
 * <p>
 * Maps domain exceptions to appropriate HTTP status codes:
 * <ul>
 *   <li>{@link NotFoundException} → 404 Not Found</li>
 *   <li>{@link IllegalArgumentException} → 400 Bad Request</li>
 *   <li>{@link FeignException} 4xx → passthrough (e.g. downstream 404)</li>
 *   <li>{@link FeignException} 5xx / connectivity → 503 Service Unavailable</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<String> handleFeignException(FeignException ex) {
        int status = ex.status();
        if (status >= 400 && status < 500) {
            return ResponseEntity.status(status).body(ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Downstream service unavailable");
    }
}
