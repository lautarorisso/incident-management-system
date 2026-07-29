package com.lautarorisso.user_service.adapter.out.keycloak.mapper;

import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakUserResponse;
import com.lautarorisso.user_service.domain.model.User;
import com.lautarorisso.user_service.domain.model.UserId;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper between Keycloak user DTOs and domain User models.
 * <p>
 * Converts the Keycloak representation to the application's
 * internal domain model for local persistence.
 */
@Component
public class KeycloakUserMapper {

    public User toDomain(KeycloakUserResponse kcUser) {
        return User.builder()
                .keycloakId(UUID.fromString(kcUser.getId()))
                .username(kcUser.getUsername())
                .displayName(buildDisplayName(kcUser))
                .email(kcUser.getEmail())
                .active(kcUser.isEnabled())
                .build();
    }

    private String buildDisplayName(KeycloakUserResponse kcUser) {
        if (kcUser.getFirstName() != null && kcUser.getLastName() != null) {
            return kcUser.getFirstName() + " " + kcUser.getLastName();
        }
        return kcUser.getUsername();
    }
}
