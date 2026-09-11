package com.lautarorisso.notification_service.config;

import com.ims.shared.security.JwtGrantedAuthoritiesConverter;
import com.ims.shared.security.SharedSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * JWT resource-server configuration for the Notification Service.
 * <p>
 * {@code com.ims.shared} sits outside this service's component-scan root
 * ({@code com.lautarorisso.notification_service}), so the shared security
 * surface is activated explicitly via {@link Import}. The filter chain is
 * built with the shared {@link SharedSecurityConfiguration} builder — no
 * reimplementation of the chain, converter, session policy or CSRF handling
 * in this service.
 * <p>
 * Authorization matrix (design: per-service role matrices):
 * <ul>
 *   <li>{@code permitAll}: actuator health/info and API docs (Scalar) — public</li>
 *   <li>{@code /api/notifications/**}: any authenticated caller. The
 *       owner-check ({@code sub == userId} or {@code ROLE_ADMIN}) lives in
 *       {@code NotificationController.getNotifications}, which must be
 *       reachable by every authenticated role to decide 200 vs 403.</li>
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
                .authorize("/api/notifications/**").authenticated()
                .build();
    }
}