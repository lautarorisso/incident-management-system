package com.lautarorisso.api_gateway.security;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Maps Keycloak realm roles to Spring Security authorities.
 * <p>
 * Keycloak stores realm roles under the custom claim
 * {@code realm_access.roles} (e.g. {@code ims-admin}), which Spring Security
 * does not read by default — it only understands {@code scope}/{@code scp}.
 * This converter picks the application roles ({@code ims-*} prefix), strips
 * the prefix and exposes them as {@code ROLE_ADMIN}/{@code ROLE_AGENT}/
 * {@code ROLE_USER}. Built-in Keycloak roles such as {@code offline_access}
 * or {@code uma_authorization} are deliberately ignored.
 */
@Component
public class JwtRoleConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";
    private static final String ROLE_PREFIX = "ims-";
    private static final String AUTHORITY_PREFIX = "ROLE_";

    private final ReactiveJwtAuthenticationConverter delegate =
            new ReactiveJwtAuthenticationConverter();

    public JwtRoleConverter() {
        delegate.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
    }

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        return delegate.convert(jwt);
    }

    private Flux<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return Flux.fromIterable(realmRoles(jwt))
                .filter(role -> role.startsWith(ROLE_PREFIX))
                .map(role -> new SimpleGrantedAuthority(
                        AUTHORITY_PREFIX + role.substring(ROLE_PREFIX.length()).toUpperCase(Locale.ROOT)));
    }

    @SuppressWarnings("unchecked")
    private List<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return List.of();
        }
        Object roles = realmAccess.get(ROLES_KEY);
        return roles instanceof List ? (List<String>) roles : List.of();
    }
}
