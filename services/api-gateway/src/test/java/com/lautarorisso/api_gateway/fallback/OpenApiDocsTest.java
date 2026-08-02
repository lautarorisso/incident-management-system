package com.lautarorisso.api_gateway.fallback;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that springdoc OpenAPI endpoints are enabled and return
 * valid responses. The gateway exposes its own API docs (including
 * the fallback controller) and configures Scalar UI to aggregate
 * downstream service specs.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.config.enabled=false"
        })
@Import(com.lautarorisso.api_gateway.TestSecurityConfig.class)
class OpenApiDocsTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void apiDocsEndpointShouldReturnOk() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.openapi").exists()
                .jsonPath("$.paths./fallback/{service}").exists();
    }

    @Test
    void scalarEndpointShouldReturnOk() {
        webTestClient.get()
                .uri("/scalar")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void scalarPageShouldListServices() {
        webTestClient.get()
                .uri("/scalar")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertThat(body).contains("API Gateway");
                    assertThat(body).contains("Incident Service");
                    assertThat(body).contains("User Service");
                    assertThat(body).contains("Notification Service");
                });
    }
}
