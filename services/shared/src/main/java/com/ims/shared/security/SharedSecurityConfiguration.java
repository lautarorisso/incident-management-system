package com.ims.shared.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Shared servlet JWT resource-server configuration for the business services.
 * <p>
 * {@code com.ims.shared} sits outside every service's component-scan root
 * ({@code com.lautarorisso.*}), so this class is never auto-registered. Each
 * service MUST activate it explicitly through {@code @Import}:
 *
 * <pre>{@code
 * @Configuration
 * @Import(SharedSecurityConfiguration.class)
 * public class SecurityConfig {
 *
 *     @Bean
 *     SecurityFilterChain filterChain(HttpSecurity http,
 *                                     JwtGrantedAuthoritiesConverter converter) throws Exception {
 *         return SharedSecurityConfiguration.builder(http)
 *                 .jwtAuthenticationConverter(converter)
 *                 .permitAll("/actuator/health", "/actuator/info", "/v3/api-docs/**", "/scalar/**")
 *                 .authorize("/api/**").hasAnyRole("ADMIN", "AGENT", "USER")
 *                 .build();
 *     }
 * }
 * }</pre>
 *
 * Without the {@code @Import}, no {@code SecurityFilterChain} is registered and
 * the service boots with Spring Security's fail-closed defaults — missing
 * validation is loud, never silent. The class is {@code final} with
 * {@code proxyBeanMethods = false} (lite mode) because it is a static factory:
 * no CGLIB subclassing is needed or wanted.
 * <p>
 * The {@link Builder} returned by {@link #builder(HttpSecurity)} wires the
 * {@link JwtGrantedAuthoritiesConverter} into the OAuth2 resource server, forces
 * a stateless session and disables CSRF (bearer-only APIs), then applies the
 * caller's permit/authorize matrix. Anything not matched explicitly falls back
 * to {@code anyRequest().authenticated()}.
 */
@Configuration(proxyBeanMethods = false)
public final class SharedSecurityConfiguration {

    private SharedSecurityConfiguration() {
    }

    /**
     * Creates a {@link SecurityFilterChain} builder for the given {@code http}.
     *
     * @param http the {@link HttpSecurity} instance injected into the service's
     *             {@code filterChain} bean method
     * @return a new builder
     */
    public static Builder builder(HttpSecurity http) {
        return new Builder(http);
    }

    /**
     * Exposes the servlet {@code ims-*} → {@code ROLE_*} converter as a bean so
     * services can inject it into their {@code filterChain} method without
     * constructing it themselves.
     *
     * @return a new {@link JwtGrantedAuthoritiesConverter}
     */
    @Bean
    public JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter() {
        return new JwtGrantedAuthoritiesConverter();
    }

    /**
     * Fluent factory for a servlet JWT resource-server {@link SecurityFilterChain}.
     * <p>
     * Usage: start from {@link SharedSecurityConfiguration#builder(HttpSecurity)},
     * declare public patterns with {@link #permitAll(String...)}, protected
     * patterns with {@link #authorize(String...)} (terminated by
     * {@code hasAnyRole(...)} or {@code authenticated()}), then
     * {@link #build()}. All remaining endpoints require authentication.
     */
    public static class Builder {

        private final HttpSecurity http;
        private final List<Rule> rules = new ArrayList<>();
        private JwtGrantedAuthoritiesConverter authoritiesConverter;

        Builder(HttpSecurity http) {
            this.http = http;
        }

        /**
         * Uses the given converter for JWT → authorities mapping. Defaults to a
         * fresh {@link JwtGrantedAuthoritiesConverter} when not called.
         *
         * @param converter the (injected) shared servlet converter
         * @return this builder
         */
        public Builder jwtAuthenticationConverter(JwtGrantedAuthoritiesConverter converter) {
            this.authoritiesConverter = converter;
            return this;
        }

        /**
         * Declares request patterns accessible without authentication.
         *
         * @param patterns Ant matchers, e.g. {@code "/actuator/health"}
         * @return this builder
         */
        public Builder permitAll(String... patterns) {
            rules.add(new Rule(null, patterns, null, false));
            return this;
        }

        /**
         * Starts a protected-request declaration for the given patterns. Terminate
         * with {@code hasAnyRole(...)} or {@code authenticated()}.
         *
         * @param patterns Ant matchers for the protected endpoints
         * @return a terminator for this declaration
         */
        public AuthorizedRequests authorize(String... patterns) {
            return new AuthorizedRequests(null, patterns);
        }

        /**
         * Starts a protected-request declaration for the given method and patterns.
         * Terminate with {@code hasAnyRole(...)} or {@code authenticated()}.
         *
         * @param method   the HTTP method the patterns apply to ({@code null} for any)
         * @param patterns Ant matchers for the protected endpoints
         * @return a terminator for this declaration
         */
        public AuthorizedRequests authorize(HttpMethod method, String... patterns) {
            return new AuthorizedRequests(method, patterns);
        }

        /**
         * Wires the resource server and the accumulated authorization matrix into
         * the {@link SecurityFilterChain}.
         *
         * @return the built chain
         * @throws Exception on any Spring Security configuration error
         */
        public SecurityFilterChain build() throws Exception {
            JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                    authoritiesConverter != null ? authoritiesConverter : new JwtGrantedAuthoritiesConverter());

            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(requests -> {
                        for (Rule rule : rules) {
                            rule.apply(requests);
                        }
                        requests.anyRequest().authenticated();
                    })
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

            return http.build();
        }

        /**
         * Terminator for an {@code authorize(...)} declaration.
         */
        public class AuthorizedRequests {

            private final HttpMethod method;
            private final String[] patterns;

            AuthorizedRequests(HttpMethod method, String... patterns) {
                this.method = method;
                this.patterns = patterns;
            }

            /**
             * Restricts the declared patterns to the given roles. Each role is
             * matched against the {@code ROLE_*} authority produced by the
             * converter (e.g. {@code "ADMIN"} → {@code ROLE_ADMIN}).
             *
             * @param roles the role names required for access
             * @return the outer builder
             */
            public Builder hasAnyRole(String... roles) {
                rules.add(new Rule(method, patterns, roles, false));
                return Builder.this;
            }

            /**
             * Restricts the declared patterns to any authenticated caller,
             * regardless of role.
             *
             * @return the outer builder
             */
            public Builder authenticated() {
                rules.add(new Rule(method, patterns, null, true));
                return Builder.this;
            }
        }
    }

    private static final class Rule {

        private final HttpMethod method;
        private final String[] patterns;
        private final String[] roles;
        private final boolean authenticated;

        Rule(HttpMethod method, String[] patterns, String[] roles, boolean authenticated) {
            this.method = method;
            this.patterns = patterns;
            this.roles = roles;
            this.authenticated = authenticated;
        }

        void apply(AuthorizeHttpRequestsConfigurer<HttpSecurity>
                           .AuthorizationManagerRequestMatcherRegistry requests) {
            if (roles != null) {
                if (method != null) {
                    requests.requestMatchers(method, patterns).hasAnyRole(roles);
                } else {
                    requests.requestMatchers(patterns).hasAnyRole(roles);
                }
            } else if (authenticated) {
                if (method != null) {
                    requests.requestMatchers(method, patterns).authenticated();
                } else {
                    requests.requestMatchers(patterns).authenticated();
                }
            } else if (method != null) {
                requests.requestMatchers(method, patterns).permitAll();
            } else {
                requests.requestMatchers(patterns).permitAll();
            }
        }
    }
}