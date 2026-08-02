package com.lautarorisso.user_service.adapter.out.keycloak;

import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Keycloak Admin client connection.
 */
@Getter
@ToString
@Component
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakProperties {
    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
}
