package com.lautarorisso.api_gateway.fallback;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Resilience4j circuit breaker configuration from
 * application.yaml is loaded correctly and all required instances exist.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@Import(com.lautarorisso.api_gateway.TestSecurityConfig.class)
class Resilience4jConfigurationTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void defaultConfigShouldHaveCorrectValues() {
        CircuitBreakerConfig defaultConfig = circuitBreakerRegistry.getConfiguration("default")
                .orElseThrow(() -> new AssertionError("default config not found"));

        assertThat(defaultConfig.getSlidingWindowSize()).isEqualTo(10);
        assertThat(defaultConfig.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(defaultConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        assertThat(defaultConfig.getFailureRateThreshold()).isEqualTo(50f);
    }

    @Test
    void apiGatewayInstanceShouldExist() {
        assertThat(circuitBreakerRegistry.find("api-gateway")).isPresent();
    }

    @Test
    void incidentServiceInstanceShouldExist() {
        assertThat(circuitBreakerRegistry.find("incident-service")).isPresent();
    }

    @Test
    void notificationServiceInstanceShouldExist() {
        assertThat(circuitBreakerRegistry.find("notification-service")).isPresent();
    }

    @Test
    void userServiceInstanceShouldExist() {
        assertThat(circuitBreakerRegistry.find("user-service")).isPresent();
    }

    @Test
    void allInstancesShouldUseDefaultConfig() {
        for (String name : new String[]{
                "api-gateway", "incident-service",
                "notification-service", "user-service"}) {
            CircuitBreakerConfig config = circuitBreakerRegistry.find(name)
                    .orElseThrow(() -> new AssertionError(name + " not found"))
                    .getCircuitBreakerConfig();

            assertThat(config.getSlidingWindowSize())
                    .as(name + " slidingWindowSize")
                    .isEqualTo(10);
        }
    }
}
