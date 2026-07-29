package com.lautarorisso.user_service.adapter.out.keycloak;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

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
        properties.setServerUrl("http://localhost:8080");
        properties.setRealm("master");
        properties.setClientId("admin-cli");
        properties.setClientSecret("secret");

        KeycloakConfig config = new KeycloakConfig(properties);
        Keycloak keycloak = config.keycloak();

        assertNotNull(keycloak);
    }

    @Test
    void keycloakConfigThrowsOnMissingServerUrl() {
        KeycloakProperties properties = new KeycloakProperties();
        properties.setServerUrl("");
        properties.setRealm("master");
        properties.setClientId("admin-cli");
        properties.setClientSecret("secret");

        KeycloakConfig config = new KeycloakConfig(properties);

        assertThrows(IllegalArgumentException.class, config::keycloak);
    }
}
