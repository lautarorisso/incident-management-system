package com.lautarorisso.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        chain = exchange -> Mono.empty();
    }

    @Test
    void shouldNotFailOnGetRequest() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldNotFailOnEmptyPath() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("").build());

        filter.filter(exchange, chain).block();
    }
}
