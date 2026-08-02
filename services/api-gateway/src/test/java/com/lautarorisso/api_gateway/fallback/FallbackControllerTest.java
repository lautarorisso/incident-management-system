package com.lautarorisso.api_gateway.fallback;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.lautarorisso.api_gateway.TestSecurityConfig;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.matchesRegex;

@WebFluxTest(controllers = FallbackController.class)
@Import(TestSecurityConfig.class)
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void incidentsFallbackShouldReturnServiceUnavailable() {
        webTestClient.get()
                .uri("/fallback/incidents")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.service").isEqualTo("incident-service")
                .jsonPath("$.status").isEqualTo("CIRCUIT_OPEN")
                .jsonPath("$.message").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void usersFallbackShouldReturnServiceUnavailable() {
        webTestClient.get()
                .uri("/fallback/users")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.service").isEqualTo("user-service")
                .jsonPath("$.status").isEqualTo("CIRCUIT_OPEN")
                .jsonPath("$.message").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void notificationsFallbackShouldReturnServiceUnavailable() {
        webTestClient.get()
                .uri("/fallback/notifications")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.service").isEqualTo("notification-service")
                .jsonPath("$.status").isEqualTo("CIRCUIT_OPEN")
                .jsonPath("$.message").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void unknownServiceFallbackShouldReturnServiceUnavailable() {
        webTestClient.get()
                .uri("/fallback/unknown")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo("CIRCUIT_OPEN")
                .jsonPath("$.message").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void timestampShouldBeIso8601() {
        webTestClient.get()
                .uri("/fallback/incidents")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.timestamp")
                .value(matchesRegex(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
    }
}
