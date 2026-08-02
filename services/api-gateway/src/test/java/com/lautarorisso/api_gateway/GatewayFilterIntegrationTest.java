package com.lautarorisso.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for gateway GlobalFilters.
 * <p>
 * GlobalFilters execute before route resolution, so we can verify filter
 * headers even when the backend is unreachable (expected 5xx error).
 * <p>
 * The X-User-Id JWT scenario is verified in {@code UserIdHeaderFilterTest}
 * since {@code mockJwt()} is not compatible with {@code RANDOM_PORT} mode.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.cloud.gateway.server.webflux.routes[0].id=filter-test-route",
                "spring.cloud.gateway.server.webflux.routes[0].uri=http://localhost:9199",
                "spring.cloud.gateway.server.webflux.routes[0].predicates=Path=/filter-test/**",
                "spring.cloud.gateway.server.webflux.routes[0].filters[0]=StripPrefix=1"
        })
@Import(TestSecurityConfig.class)
class GatewayFilterIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void correlationIdFilterShouldGenerateIdWhenMissing() {
        webTestClient.get()
                .uri("/filter-test/headers")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().exists("X-Correlation-Id");
    }

    @Test
    void correlationIdFilterShouldPreserveExistingId() {
        String correlationId = "test-correlation-456";

        webTestClient.get()
                .uri("/filter-test/headers")
                .header("X-Correlation-Id", correlationId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().valueEquals("X-Correlation-Id", correlationId);
    }

    @Test
    void correlationIdFilterShouldGenerateValidUuid() {
        webTestClient.get()
                .uri("/filter-test/headers")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().value("X-Correlation-Id", value ->
                        assertThat(value)
                                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}" +
                                        "-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void userIdHeaderFilterShouldNotSetHeaderWithoutAuth() {
        webTestClient.get()
                .uri("/filter-test/headers")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().doesNotExist("X-User-Id");
    }
}
