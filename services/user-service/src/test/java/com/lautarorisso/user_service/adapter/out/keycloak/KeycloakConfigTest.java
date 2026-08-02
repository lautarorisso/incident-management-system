package com.lautarorisso.user_service.adapter.out.keycloak;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakConfigTest {

    @Test
    void keycloakBuilderCreatesInstance() {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:8080")
                .realm("master")
                .clientId("admin-cli")
                .clientSecret("test-secret")
                .grantType("client_credentials")
                .build();

        assertNotNull(keycloak);
    }

    @Test
    void keycloakConfigCreatesNonNullKeycloakBean() {
        KeycloakProperties properties = new KeycloakProperties();
        setProperties(properties, "http://localhost:8080", "master", "admin-cli", "secret");

        KeycloakConfig config = new KeycloakConfig(properties);
        Keycloak keycloak = config.keycloak();

        assertNotNull(keycloak);
    }

    @Test
    void keycloakConfigThrowsOnMissingServerUrl() {
        KeycloakProperties properties = new KeycloakProperties();
        setProperties(properties, "", "master", "admin-cli", "secret");

        KeycloakConfig config = new KeycloakConfig(properties);

        assertThrows(IllegalArgumentException.class, config::keycloak);
    }

    /**
     * KeycloakProperties is a read-only configuration bean (getters only); Spring Boot binds
     * its values directly to the private fields, so tests set them reflectively.
     */
    private void setProperties(KeycloakProperties properties, String serverUrl, String realm,
                               String clientId, String clientSecret) {
        ReflectionTestUtils.setField(properties, "serverUrl", serverUrl);
        ReflectionTestUtils.setField(properties, "realm", realm);
        ReflectionTestUtils.setField(properties, "clientId", clientId);
        ReflectionTestUtils.setField(properties, "clientSecret", clientSecret);
    }
}
