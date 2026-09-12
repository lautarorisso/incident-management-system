package com.lautarorisso.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

/**
 * Authorization matrix for the gateway security rules.
 * <p>
 * A stub {@link ReactiveJwtDecoder} mints tokens whose value encodes the
 * caller's realm roles ({@code subject_role1+role2}), so the full chain
 * (decoder → role converter → route rules) is exercised without Keycloak or
 * network. Requests that pass authorization fail downstream with a 5xx
 * (routes point at unreachable backends), which is how "allowed" is asserted.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        // Test resources application.yaml replaces the main one (same
        // classpath name), so routes must be declared here. The stub backend
        // is unreachable on purpose: requests past security fail with 5xx.
        "spring.cloud.gateway.server.webflux.routes[0].id=authz-test-route",
        "spring.cloud.gateway.server.webflux.routes[0].uri=http://localhost:9199",
        "spring.cloud.gateway.server.webflux.routes[0].predicates=Path=/api/**",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration"
})
@Import(GatewayAuthorizationTest.StubJwtDecoderConfig.class)
class GatewayAuthorizationTest {

    // Tokens use only RFC 6750 b64token-safe characters (_ and +), since
    // ServerBearerTokenAuthenticationConverter rejects anything else.
    private static final String ADMIN = "Bearer admin_ims-admin+default-roles-ims";
    private static final String AGENT = "Bearer agent_ims-agent";
    private static final String USER = "Bearer user_ims-user";

    @Autowired
    private ApplicationContext context;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @Test
    void missingTokenIsRejected() {
        client.get().uri("/api/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void userRoleIsForbiddenOnUserDirectory() {
        client.get().uri("/api/users").header(HttpHeaders.AUTHORIZATION, USER)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void agentRoleCanListUsers() {
        expectPastSecurity(() -> client.get().uri("/api/users").header(HttpHeaders.AUTHORIZATION, AGENT));
    }

    @Test
    void adminRoleCanListUsers() {
        expectPastSecurity(() -> client.get().uri("/api/users").header(HttpHeaders.AUTHORIZATION, ADMIN));
    }

    @Test
    void anyAuthenticatedRoleCanCreateIncidents() {
        expectPastSecurity(() -> client.post().uri("/api/incidents")
                .header(HttpHeaders.AUTHORIZATION, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("title", "t")));
    }

    @Test
    void agentRoleCanTransitionIncidents() {
        expectPastSecurity(() -> client.put()
                .uri("/api/incidents/00000000-0000-0000-0000-000000000001/transition")
                .header(HttpHeaders.AUTHORIZATION, AGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", "IN_PROGRESS")));
    }

    @Test
    void userRoleCannotAssignIncidents() {
        client.put().uri("/api/incidents/00000000-0000-0000-0000-000000000001/assign")
                .header(HttpHeaders.AUTHORIZATION, USER)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("assigneeId", "00000000-0000-0000-0000-000000000002"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void apiDocsRemainPublic() {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(401).isNotEqualTo(403));
    }

    @Test
    void eurekaIsNoLongerPubliclyAccessible() {
        client.get().uri("/eureka/")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void eurekaRouteIsRemovedForAuthenticatedCallers() {
        client.get().uri("/eureka/").header(HttpHeaders.AUTHORIZATION, ADMIN)
                .exchange()
                .expectStatus().isNotFound();
    }

    private void expectPastSecurity(Supplier<WebTestClient.RequestHeadersSpec<?>> request) {
        request.get().exchange()
                .expectStatus().value(status -> assertThat(status).isGreaterThanOrEqualTo(500));
    }

    @TestConfiguration
    static class StubJwtDecoderConfig {

        /**
         * Token format: {@code subject:role1,role2}. The decoder turns that
         * into a real {@link Jwt} carrying {@code realm_access.roles}, so the
         * production {@code JwtRoleConverter} does the mapping.
         */
        /**
         * Token format: {@code subject_role1+role2}. The decoder turns that
         * into a real {@link Jwt} carrying {@code realm_access.roles}, so the
         * production {@code JwtRoleConverter} does the mapping.
         */
        @Bean
        ReactiveJwtDecoder stubJwtDecoder() {
            return token -> {
                int separator = token.indexOf('_');
                String subject = separator < 0 ? token : token.substring(0, separator);
                List<String> roles = separator < 0 ? List.of()
                        : Arrays.asList(token.substring(separator + 1).split("\\+"));
                Jwt jwt = Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .claim("sub", subject)
                        .claim("realm_access", Map.of("roles", roles))
                        .build();
                return Mono.just(jwt);
            };
        }
    }
}
