package com.lautarorisso.incident_service.adapter.out.feign.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j CircuitBreaker configuration for Feign clients.
 * <p>
 * Defines a custom circuit breaker configuration for the user-service Feign client.
 * In production, these settings are also overridable via application.yaml
 * under {@code resilience4j.circuitbreaker.instances.user-service}.
 */
@Configuration
public class CircuitBreakerConfig {

    @Bean
    public io.github.resilience4j.circuitbreaker.CircuitBreakerConfig userServiceCircuitBreakerConfig() {
        return io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .failureRateThreshold(50)
                .build();
    }
}
