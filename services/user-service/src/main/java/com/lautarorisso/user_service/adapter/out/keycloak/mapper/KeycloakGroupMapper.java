package com.lautarorisso.user_service.adapter.out.keycloak.mapper;

import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakGroupResponse;
import com.lautarorisso.user_service.domain.model.Team;
import org.springframework.stereotype.Component;

/**
 * Mapper between Keycloak group DTOs and domain Team models.
 * <p>
 * Converts the Keycloak group representation to the application's
 * internal domain model for local persistence.
 */
@Component
public class KeycloakGroupMapper {

    public Team toDomain(KeycloakGroupResponse kcGroup) {
        return Team.builder()
                .name(kcGroup.getName())
                .description(kcGroup.getPath() != null
                        ? "Keycloak group: " + kcGroup.getPath()
                        : "Keycloak group: " + kcGroup.getName())
                .build();
    }
}
