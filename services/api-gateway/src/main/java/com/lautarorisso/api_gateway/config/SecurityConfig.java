package com.lautarorisso.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.lautarorisso.api_gateway.security.JwtRoleConverter;

/**
 * JWT resource server configuration with role-based authorization.
 *
 * Tokens issued by the Keycloak {@code ims} realm are validated against the
 * issuer's public keys (see
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} in the config
 * server) and realm roles are mapped to authorities by
 * {@link JwtRoleConverter}. Actuator, API docs and Scalar stay public; the
 * Eureka dashboard is no longer exposed through the gateway.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            JwtRoleConverter jwtRoleConverter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**", "/scalar", "/scalar/**", "/v3/api-docs/**").permitAll()
                        // Incident creation is open to every authenticated role.
                        .pathMatchers(HttpMethod.POST, "/api/incidents").hasAnyRole("ADMIN", "AGENT", "USER")
                        // Workflow operations are for staff only.
                        .pathMatchers("/api/incidents/*/assign", "/api/incidents/*/transition")
                        .hasAnyRole("ADMIN", "AGENT")
                        // Directory lookups back the assignment workflow.
                        .pathMatchers("/api/users/**", "/api/teams/**").hasAnyRole("ADMIN", "AGENT")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtRoleConverter)))
                .build();
    }
}
