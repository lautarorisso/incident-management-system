package com.lautarorisso.api_gateway.fallback;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * DTO for circuit breaker fallback responses returned by the API Gateway.
 * Contains the downstream service name, circuit status, a user-facing message,
 * and an ISO-8601 timestamp.
 */
public record FallbackResponse(
        String service,
        String status,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {

    /**
     * Creates a fallback response for the given service with CIRCUIT_OPEN status.
     */
    public static FallbackResponse forService(String serviceName) {
        return new FallbackResponse(
                serviceName,
                "CIRCUIT_OPEN",
                serviceName + " is currently unavailable. Please try again later.",
                LocalDateTime.now()
        );
    }
}
