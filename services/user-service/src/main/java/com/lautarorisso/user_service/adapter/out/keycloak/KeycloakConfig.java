package com.lautarorisso.user_service.adapter.out.keycloak;

import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Keycloak Admin client as a Spring bean.
 * <p>
 * The client uses client credentials grant to authenticate
 * against the Keycloak server for admin operations.
 */
@Configuration
@RequiredArgsConstructor
public class KeycloakConfig {

    private final KeycloakProperties properties;

    @Bean
    public Keycloak keycloak() {
        String serverUrl = properties.getServerUrl();
        String realm = properties.getRealm();
        String clientId = properties.getClientId();
        String clientSecret = properties.getClientSecret();

        if (serverUrl == null || serverUrl.isBlank()) {
            throw new IllegalArgumentException("keycloak.admin.server-url must be configured");
        }
        if (realm == null || realm.isBlank()) {
            throw new IllegalArgumentException("keycloak.admin.realm must be configured");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("keycloak.admin.client-id must be configured");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("keycloak.admin.client-secret must be configured");
        }

        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType("client_credentials")
                .build();
    }
}
