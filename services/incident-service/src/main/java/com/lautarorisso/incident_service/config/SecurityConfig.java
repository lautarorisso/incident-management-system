package com.lautarorisso.incident_service.config;

import com.ims.shared.security.JwtGrantedAuthoritiesConverter;
import com.ims.shared.security.SharedSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * JWT resource-server configuration for the Incident Service.
 * <p>
 * {@code com.ims.shared} sits outside this service's component-scan root
 * ({@code com.lautarorisso.incident_service}), so the shared security surface
 * is activated explicitly via {@link Import}. The filter chain is built with
 * the shared {@link SharedSecurityConfiguration} builder — no reimplementation
 * of the chain, converter, session policy or CSRF handling in this service.
 * <p>
 * Authorization matrix (design: per-service role matrices):
 * <ul>
 *   <li>{@code permitAll}: actuator health/info and API docs (Scalar) — public</li>
 *   <li>{@code PUT} assign and transition of a single incident: requires
 *       {@code ADMIN} or {@code AGENT} — state-changing operations with
 *       downstream side effects (Feign calls to user-service, outbox events)</li>
 *   <li>{@code GET} incident list: {@code ADMIN} or {@code AGENT}</li>
 *   <li>{@code POST} incident creation: any authenticated caller</li>
 *   <li>everything else (e.g. fetching a single incident): any authenticated
 *       caller</li>
 * </ul>
 */
@Configuration
@Import(SharedSecurityConfiguration.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtGrantedAuthoritiesConverter converter) throws Exception {
        return SharedSecurityConfiguration.builder(http)
                .jwtAuthenticationConverter(converter)
                .permitAll("/actuator/health", "/actuator/info", "/v3/api-docs/**", "/scalar/**")
                .authorize(HttpMethod.PUT, "/api/incidents/*/assign", "/api/incidents/*/transition")
                .hasAnyRole("ADMIN", "AGENT")
                .authorize(HttpMethod.GET, "/api/incidents")
                .hasAnyRole("ADMIN", "AGENT")
                .authorize(HttpMethod.POST, "/api/incidents")
                .authenticated()
                .build();
    }
}