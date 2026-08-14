package com.lautarorisso.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Permissive security configuration.
 *
 * JWT validation is disabled until Keycloak is fully integrated. Every
 * request is permitted for local/homelab development.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**", "/scalar", "/scalar/**", "/v3/api-docs/**", "/eureka/**").permitAll()
                        .anyExchange().permitAll())
                .build();
    }
}
