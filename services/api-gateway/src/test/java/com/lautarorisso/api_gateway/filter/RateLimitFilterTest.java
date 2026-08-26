package com.lautarorisso.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(2, Duration.ofSeconds(1));
        chain = exchange -> Mono.empty();
    }

    @Test
    void shouldAllowRequestWithinLimit() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents")
                        .remoteAddress(java.net.InetSocketAddress.createUnresolved("10.0.0.1", 8080))
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldBlockRequestWhenLimitExceeded() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/incidents")
                .remoteAddress(java.net.InetSocketAddress.createUnresolved("10.0.0.2", 8080))
                .build();

        ServerWebExchange exchange1 = MockServerWebExchange.from(request);
        filter.filter(exchange1, chain).block();

        ServerWebExchange exchange2 = MockServerWebExchange.from(request);
        filter.filter(exchange2, chain).block();

        ServerWebExchange exchange3 = MockServerWebExchange.from(request);
        filter.filter(exchange3, chain).block();

        assertThat(exchange3.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldAllowDifferentClientsIndependently() {
        ServerWebExchange client1 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents")
                        .remoteAddress(java.net.InetSocketAddress.createUnresolved("10.0.0.3", 8080))
                        .build());
        ServerWebExchange client2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents")
                        .remoteAddress(java.net.InetSocketAddress.createUnresolved("10.0.0.4", 8080))
                        .build());

        filter.filter(client1, chain).block();
        filter.filter(client1, chain).block();

        ServerWebExchange client1Blocked = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents")
                        .remoteAddress(java.net.InetSocketAddress.createUnresolved("10.0.0.3", 8080))
                        .build());
        filter.filter(client1Blocked, chain).block();
        assertThat(client1Blocked.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        ServerWebExchange client2Allowed = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents")
                        .remoteAddress(java.net.InetSocketAddress.createUnresolved("10.0.0.4", 8080))
                        .build());
        filter.filter(client2Allowed, chain).block();
        assertThat(client2Allowed.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldUseXForwardedForAsClientKey() {
        MockServerHttpRequest request1 = MockServerHttpRequest.get("/api/incidents")
                .header("X-Forwarded-For", "192.168.1.1")
                .remoteAddress(java.net.InetSocketAddress.createUnresolved("10.0.0.5", 8080))
                .build();

        filter.filter(MockServerWebExchange.from(request1), chain).block();
        filter.filter(MockServerWebExchange.from(request1), chain).block();

        MockServerHttpRequest request3 = MockServerHttpRequest.get("/api/incidents")
                .header("X-Forwarded-For", "192.168.1.1")
                .remoteAddress(java.net.InetSocketAddress.createUnresolved("10.0.0.5", 8080))
                .build();
        ServerWebExchange exchange3 = MockServerWebExchange.from(request3);
        filter.filter(exchange3, chain).block();

        assertThat(exchange3.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
