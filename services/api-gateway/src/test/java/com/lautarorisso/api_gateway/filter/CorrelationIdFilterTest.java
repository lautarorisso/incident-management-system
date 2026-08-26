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

    private CorrelationIdFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        chain = exchange -> Mono.empty();
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());

        filter.filter(exchange, chain).block();

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
        String existingId = "existing-id-123";
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents")
                        .header(CORRELATION_ID_HEADER, existingId)
                        .build());

        filter.filter(exchange, chain).block();

        String correlationId = exchange.getResponse().getHeaders()
                .getFirst(CORRELATION_ID_HEADER);
        assertThat(correlationId).isEqualTo(existingId);
    }
}
