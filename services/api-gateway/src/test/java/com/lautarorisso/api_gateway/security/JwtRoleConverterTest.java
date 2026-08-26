package com.lautarorisso.api_gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRoleConverterTest {

    private final JwtRoleConverter converter = new JwtRoleConverter();

    @Test
    void shouldMapImsRolesToStrippedAuthorities() {
        var auth = converter.convert(jwtWithRoles("ims-admin", "ims-agent", "ims-user")).block();

        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_AGENT", "ROLE_USER");
    }

    @Test
    void shouldIgnoreBuiltInKeycloakRoles() {
        var auth = converter.convert(
                jwtWithRoles("offline_access", "uma_authorization", "default-roles-ims")).block();

        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void shouldIgnoreUnknownImsPrefixedRolesButKeepKnownOnes() {
        var auth = converter.convert(jwtWithRoles("ims-audit", "ims-user")).block();

        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_AUDIT", "ROLE_USER");
    }

    @Test
    void shouldReturnNoAuthoritiesWhenClaimIsMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "someone")
                .build();

        var auth = converter.convert(jwt).block();

        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void shouldReturnNoAuthoritiesWhenRolesEntryIsNotAList() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "someone")
                .claim("realm_access", Map.of("roles", "not-a-list"))
                .build();

        var auth = converter.convert(jwt).block();

        assertThat(auth.getAuthorities()).isEmpty();
    }

    private Jwt jwtWithRoles(String... roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "someone")
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();
    }
}
