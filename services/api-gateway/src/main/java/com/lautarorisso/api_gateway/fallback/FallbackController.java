package com.lautarorisso.api_gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Circuit breaker fallback controller.
 * <p>
 * Returns a 503 (Service Unavailable) response with a JSON body describing
 * which downstream service is unavailable. Route-level circuit breakers in
 * the gateway configuration forward to {@code forward:/fallback/{service}}
 * when the circuit is open.
 */
@RestController
public class FallbackController {

    private static final Map<String, String> SERVICE_NAMES = Map.of(
            "incidents", "incident-service",
            "users", "user-service",
            "notifications", "notification-service"
    );

    @GetMapping("/fallback/{service}")
    public ResponseEntity<FallbackResponse> fallback(
            @PathVariable String service) {
        String serviceName = SERVICE_NAMES.getOrDefault(
                service, service + "-service");
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(FallbackResponse.forService(serviceName));
    }
}
