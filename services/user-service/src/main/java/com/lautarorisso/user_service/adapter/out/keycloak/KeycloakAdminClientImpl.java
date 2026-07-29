package com.lautarorisso.user_service.adapter.out.keycloak;

import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakGroupResponse;
import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakUserResponse;
import com.lautarorisso.user_service.domain.port.out.KeycloakAdminClient;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Adapter that implements KeycloakAdminClient using the Keycloak Admin client library.
 * <p>
 * Fetches users, groups, and user-group memberships from Keycloak via the Admin REST API.
 */
@Slf4j
@Component
public class KeycloakAdminClientImpl implements KeycloakAdminClient {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakAdminClientImpl(Keycloak keycloak,
                                   @Value("${keycloak.admin.realm}") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    @Override
    public List<KeycloakUserResponse> fetchUsers() {
        try {
            return keycloak.realm(realm).users().list().stream()
                    .map(this::mapUser)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch users from Keycloak", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<KeycloakGroupResponse> fetchGroups() {
        try {
            return keycloak.realm(realm).groups().groups().stream()
                    .map(this::mapGroup)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch groups from Keycloak", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<KeycloakGroupResponse> fetchUserGroups(UUID userId) {
        try {
            return keycloak.realm(realm).users().get(userId.toString()).groups().stream()
                    .map(this::mapGroup)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch user groups from Keycloak for userId: {}", userId, e);
            return Collections.emptyList();
        }
    }

    private KeycloakUserResponse mapUser(org.keycloak.representations.idm.UserRepresentation rep) {
        return KeycloakUserResponse.builder()
                .id(rep.getId())
                .username(rep.getUsername())
                .firstName(rep.getFirstName())
                .lastName(rep.getLastName())
                .email(rep.getEmail())
                .enabled(rep.isEnabled())
                .attributes(rep.getAttributes())
                .build();
    }

    private KeycloakGroupResponse mapGroup(org.keycloak.representations.idm.GroupRepresentation rep) {
        return KeycloakGroupResponse.builder()
                .id(rep.getId())
                .name(rep.getName())
                .path(rep.getPath())
                .build();
    }
}
