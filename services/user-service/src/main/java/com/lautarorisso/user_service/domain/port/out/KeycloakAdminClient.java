package com.lautarorisso.user_service.domain.port.out;

import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakGroupResponse;
import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakUserResponse;

import java.util.List;
import java.util.UUID;

/**
 * Driven port (outbound) for fetching users and groups from Keycloak.
 * <p>
 * The Keycloak adapter implements this interface to provide sync capabilities.
 */
public interface KeycloakAdminClient {

    List<KeycloakUserResponse> fetchUsers();

    List<KeycloakGroupResponse> fetchGroups();

    List<KeycloakGroupResponse> fetchUserGroups(UUID userId);
}
