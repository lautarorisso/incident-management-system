package com.lautarorisso.api_gateway;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Test security configuration that permits all requests.
 * Used by GatewayFilterIntegrationTest to bypass JWT authentication.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(auth -> auth
                        .anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
