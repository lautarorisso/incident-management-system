package com.lautarorisso.user_service.exception;

import com.ims.shared.exception.BaseGlobalExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for user-service REST endpoints.
 * <p>
 * Extends {@link BaseGlobalExceptionHandler} for common exceptions
 * (404, 400, 409, 502, validation, catch-all).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {
}
