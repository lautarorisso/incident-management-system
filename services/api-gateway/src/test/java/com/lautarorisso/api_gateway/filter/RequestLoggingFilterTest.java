package com.lautarorisso.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        chain = exchange -> Mono.empty();
    }

    @Test
    void shouldLogMethodAndPath(CapturedOutput output) {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());

        filter.filter(exchange, chain).block();

        assertThat(output).contains("GET /api/incidents");
    }

    @Test
    void shouldLogStatusWhenAvailable(CapturedOutput output) {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());
        chain = e -> {
            e.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(output).contains("-> 200");
    }

    @Test
    void shouldLogZeroStatusWhenNoStatusSet(CapturedOutput output) {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());

        filter.filter(exchange, chain).block();

        assertThat(output).contains("-> 0");
    }

    @Test
    void shouldNotFailOnEmptyPath(CapturedOutput output) {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("").build());

        filter.filter(exchange, chain).block();

        assertThat(output).contains("GET");
    }
}