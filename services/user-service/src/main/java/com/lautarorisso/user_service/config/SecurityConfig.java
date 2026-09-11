package com.lautarorisso.user_service.config;

import com.ims.shared.security.JwtGrantedAuthoritiesConverter;
import com.ims.shared.security.SharedSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * JWT resource-server configuration for the User Service.
 * <p>
 * {@code com.ims.shared} sits outside this service's component-scan root
 * ({@code com.lautarorisso.user_service}), so the shared security surface is
 * activated explicitly via {@link Import}. The filter chain is built with the
 * shared {@link SharedSecurityConfiguration} builder — no reimplementation of
 * the chain, converter, session policy or CSRF handling in this service.
 * <p>
 * Authorization matrix (design: per-service role matrices):
 * <ul>
 *   <li>{@code permitAll}: actuator health/info and API docs (Scalar) — public</li>
 *   <li>{@code ADMIN} or {@code AGENT} (from {@code ims-admin}/{@code ims-agent}
 *       realm roles): {@code /api/users/**} and {@code /api/teams/**} — the user
 *       and team profile endpoints (also the Feign-invoked endpoints called by
 *       incident-service with the relayed end-user JWT); no service-account
 *       tokens</li>
 *   <li>everything else: any authenticated caller</li>
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
                .authorize("/api/users/**", "/api/teams/**").hasAnyRole("ADMIN", "AGENT")
                .build();
    }
}