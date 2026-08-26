package com.lautarorisso.incident_service.exception;

import com.ims.shared.exception.BaseGlobalExceptionHandler;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for incident-service REST endpoints.
 * <p>
 * Extends {@link BaseGlobalExceptionHandler} for common exceptions
 * and adds Feign-specific error handling:
 * <ul>
 *   <li>{@link FeignException} 4xx → passthrough (e.g. downstream 404)</li>
 *   <li>{@link FeignException} 5xx / connectivity → 503 Service Unavailable</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    protected ProblemDetail handleFeignException(FeignException ex) {
        int status = ex.status();
        if (status >= 400 && status < 500) {
            log.warn("Downstream 4xx from Feign: status={}, message={}", status, ex.getMessage());
            return buildProblemDetail(HttpStatus.valueOf(status), sanitizeFeignMessage(ex));
        }
        log.error("Downstream 5xx/connectivity error from Feign: status={}, message={}", status, ex.getMessage());
        return buildProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Downstream service unavailable");
    }

    private String sanitizeFeignMessage(FeignException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return "Downstream service error";
        }
        int newlineIndex = message.indexOf('\n');
        if (newlineIndex > 0) {
            return message.substring(0, newlineIndex).trim();
        }
        return message;
    }
}
