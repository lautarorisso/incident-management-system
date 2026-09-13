package com.ims.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base exception handler providing consistent RFC 7807 Problem Detail responses.
 * <p>
 * Services extend this class and add their own domain-specific handlers
 * (e.g. {@code FeignException} for services that use OpenFeign).
 * <p>
 * All error responses follow the {@link ProblemDetail} format:
 * <pre>
 * {
 *   "type": "about:blank",
 *   "title": "Not Found",
 *   "status": 404,
 *   "detail": "Incident not found: abc-123"
 * }
 * </pre>
 */
@Slf4j
public abstract class BaseGlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    protected ProblemDetail handleNotFoundException(NotFoundException ex) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    protected ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NotificationDeliveryException.class)
    protected ProblemDetail handleNotificationDeliveryException(NotificationDeliveryException ex) {
        return buildProblemDetail(HttpStatus.BAD_GATEWAY, "Notification delivery failed: " + ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        "rejectedValue", fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : ""))
                .toList();
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ProblemDetail handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST,
                "Required request parameter '" + ex.getParameterName() + "' is not present");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        return buildProblemDetail(HttpStatus.BAD_REQUEST,
                "Parameter '" + ex.getName() + "' expects type " + requiredType
                        + " but was '" + Objects.toString(ex.getValue()) + "'");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(ResponseStatusException.class)
    protected ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return buildProblemDetail(status, ex.getReason());
    }

    @ExceptionHandler(Exception.class)
    protected ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    protected ProblemDetail buildProblemDetail(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
