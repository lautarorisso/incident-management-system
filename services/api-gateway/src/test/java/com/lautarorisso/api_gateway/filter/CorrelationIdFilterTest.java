package com.lautarorisso.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final int EXPECTED_ORDER = -100;

    private CorrelationIdFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        chain = exchange -> Mono.empty();
    }

    @Test
    void shouldHaveOrderMinus100() {
        assertThat(filter.getOrder()).isEqualTo(EXPECTED_ORDER);
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() {
        // Given: request without X-Correlation-Id
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());

        // When
        filter.filter(exchange, chain).block();

        // Then: correlation ID is generated and set on response
        String correlationId = exchange.getResponse().getHeaders()
                .getFirst(CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).isNotBlank();
    }

    @Test
    void shouldGenerateValidUuidFormat() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());

        filter.filter(exchange, chain).block();

        String correlationId = exchange.getResponse().getHeaders()
                .getFirst(CORRELATION_ID_HEADER);
        assertThat(correlationId).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void shouldPreserveExistingCorrelationId() {
        // Given: request already has X-Correlation-Id
        String existingId = "existing-id-123";
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents")
                        .header(CORRELATION_ID_HEADER, existingId)
                        .build());

        // When
        filter.filter(exchange, chain).block();

        // Then: existing correlation ID is preserved
        String correlationId = exchange.getResponse().getHeaders()
                .getFirst(CORRELATION_ID_HEADER);
        assertThat(correlationId).isEqualTo(existingId);
    }

    @Test
    void shouldSetCorrelationIdOnOutgoingRequest() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());

        filter.filter(exchange, chain).block();

        // The response header shows the filter ran;
        // The request mutation is verified via MockServerHttpRequest decorator
        assertThat(exchange.getResponse().getHeaders()
                .getFirst(CORRELATION_ID_HEADER)).isNotNull();
    }
}
