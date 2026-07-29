package com.lautarorisso.api_gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.core.context.ReactiveSecurityContextHolder.withAuthentication;

class UserIdHeaderFilterTest {

    private static final String USER_ID_HEADER = "X-User-Id";

    private UserIdHeaderFilter filter;
    private AtomicReference<ServerWebExchange> capturedExchange;

    @BeforeEach
    void setUp() {
        filter = new UserIdHeaderFilter();
        capturedExchange = new AtomicReference<>();
    }

    @Test
    void shouldHaveOrderMinus60() {
        assertThat(filter.getOrder()).isEqualTo(-60);
    }

    @Test
    void shouldExtractSubClaimAndSetHeader() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-abc-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        Authentication auth = new JwtAuthenticationToken(jwt);

        runFilterWithCapture(auth);

        String userId = capturedExchange.get().getRequest()
                .getHeaders().getFirst(USER_ID_HEADER);
        assertThat(userId).isEqualTo("user-abc-123");
    }

    @Test
    void shouldNotSetHeaderWhenNoAuth() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());
        GatewayFilterChain chain = ex -> {
            capturedExchange.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String userId = capturedExchange.get().getRequest()
                .getHeaders().getFirst(USER_ID_HEADER);
        assertThat(userId).isNull();
    }

    @Test
    void shouldNotFailWhenJwtHasNoSubClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("scope", "read")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        Authentication auth = new JwtAuthenticationToken(jwt);

        runFilterWithCapture(auth);

        // Filter should pass through without setting header
        assertThat(capturedExchange.get()).isNotNull();
    }

    @Test
    void shouldSetHeaderOnMutatedRequestWithDifferentUser() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-xyz-789")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        Authentication auth = new JwtAuthenticationToken(jwt);

        runFilterWithCapture(auth);

        assertThat(capturedExchange.get().getRequest()
                .getHeaders().getFirst(USER_ID_HEADER))
                .isEqualTo("user-xyz-789");
    }

    private void runFilterWithCapture(Authentication auth) {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/incidents").build());
        GatewayFilterChain chain = ex -> {
            capturedExchange.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain)
                .contextWrite(withAuthentication(auth))
                .block();
    }
}
