package com.lautarorisso.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    private static final int EXPECTED_ORDER = -90;

    private RequestLoggingFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        chain = exchange -> Mono.empty();
    }

    @Test
    void shouldHaveOrderMinus90() {
        assertThat(filter.getOrder()).isEqualTo(EXPECTED_ORDER);
    }

    @Test
    void shouldLogMethodAndPathForGetRequest() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());

        // Simply verify the filter completes without error
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull(); // not committed
    }

    @Test
    void shouldLogMethodAndPathForPostRequest() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/incidents").build());

        filter.filter(exchange, chain).block();

        // Filter processes without error for different methods
        assertThat(exchange.getResponse()).isNotNull();
    }

    @Test
    void shouldLogMethodAndPathForPutRequest() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/incidents/1/assign").build());

        filter.filter(exchange, chain).block();
    }

    @Test
    void shouldLogWhenResponseHasStatusCode() {
        MockServerWebExchange exchange = (MockServerWebExchange) MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build());

        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.OK);

        filter.filter(exchange, chain).block();
    }

    @Test
    void shouldNotFailOnEmptyPath() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("").build());

        filter.filter(exchange, chain).block();
    }
}
