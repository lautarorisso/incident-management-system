package com.ims.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit tests for {@link JwtGrantedAuthoritiesConverter} — plain JUnit, no Spring
 * context. Covers the {@code realm_access.roles} → {@code ROLE_*} mapping table
 * from the role-mapping spec.
 */
class JwtGrantedAuthoritiesConverterTest {

    private final JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();

    @ParameterizedTest
    @CsvSource({
            "ims-admin, ROLE_ADMIN",
            "ims-agent, ROLE_AGENT",
            "ims-user, ROLE_USER"
    })
    void shouldMapSingleRealmRoleToSpringRole(String imsRole, String expectedAuthority) {
        var authorities = converter.convert(jwtWithRoles(imsRole));

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(expectedAuthority);
    }

    @Test
    void shouldIgnoreBuiltInKeycloakRoles() {
        var authorities = converter.convert(
                jwtWithRoles("offline_access", "uma_authorization", "default-roles-ims"));

        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldMapMultipleImsRolesToMultipleAuthorities() {
        var authorities = converter.convert(jwtWithRoles("ims-admin", "ims-agent", "ims-user"));

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_AGENT", "ROLE_USER");
    }

    @Test
    void shouldMapKnownAndUnknownImsPrefixedRoles() {
        var authorities = converter.convert(jwtWithRoles("ims-audit", "ims-user"));

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_AUDIT", "ROLE_USER");
    }

    @Test
    void shouldReturnNoAuthoritiesWhenRealmAccessClaimIsMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "someone")
                .build();

        var authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldReturnNoAuthoritiesWhenRolesEntryIsEmpty() {
        var authorities = converter.convert(jwtWithRoles());

        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldReturnNoAuthoritiesWhenRolesEntryIsNotAList() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", "not-a-list"))
                .build();

        var authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    private Jwt jwtWithRoles(String... roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "someone")
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .build();
    }
}