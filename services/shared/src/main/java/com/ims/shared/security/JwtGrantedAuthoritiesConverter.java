package com.ims.shared.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps Keycloak realm roles to Spring Security authorities on the servlet stack.
 * <p>
 * Keycloak stores realm roles under the custom claim {@code realm_access.roles}
 * (e.g. {@code ims-admin}), which Spring Security does not read by default — it
 * only understands {@code scope}/{@code scp}. This converter picks the
 * application roles ({@code ims-*} prefix), strips the prefix and exposes them
 * as {@code ROLE_ADMIN}/{@code ROLE_AGENT}/{@code ROLE_USER}. Built-in Keycloak
 * roles such as {@code offline_access}, {@code uma_authorization} or
 * {@code default-roles-ims} are deliberately ignored.
 * <p>
 * This is the servlet counterpart of the gateway's reactive
 * {@code JwtRoleConverter}: both apply the exact same mapping, so a token
 * validated at the gateway yields identical authorities in the business
 * services.
 */
public class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";
    private static final String ROLE_PREFIX = "ims-";
    private static final String AUTHORITY_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = realmRoles(jwt).stream()
                .filter(role -> role.startsWith(ROLE_PREFIX))
                .map(role -> new SimpleGrantedAuthority(
                        AUTHORITY_PREFIX + role.substring(ROLE_PREFIX.length()).toUpperCase(Locale.ROOT)))
                .collect(Collectors.toSet());
        return authorities;
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