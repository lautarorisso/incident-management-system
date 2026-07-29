package com.lautarorisso.user_service.adapter.out.keycloak.mapper;

import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakUserResponse;
import com.lautarorisso.user_service.domain.model.User;
import com.lautarorisso.user_service.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakUserMapperTest {

    private final KeycloakUserMapper mapper = new KeycloakUserMapper();

    @Test
    void mapsKeycloakUserToDomainUser() {
        String keycloakId = UUID.randomUUID().toString();
        KeycloakUserResponse kcUser = KeycloakUserResponse.builder()
                .id(keycloakId)
                .username("jdoe")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .enabled(true)
                .build();

        User user = mapper.toDomain(kcUser);

        assertNull(user.getId()); // ID is assigned by persistence layer
        assertEquals(keycloakId, user.getKeycloakId().toString());
        assertEquals("jdoe", user.getUsername());
        assertEquals("John Doe", user.getDisplayName());
        assertEquals("john@example.com", user.getEmail());
        assertTrue(user.isActive());
    }

    @Test
    void mapsDisabledKeycloakUserToInactiveUser() {
        KeycloakUserResponse kcUser = KeycloakUserResponse.builder()
                .id(UUID.randomUUID().toString())
                .username("inactive_user")
                .firstName("Inactive")
                .lastName("User")
                .email("inactive@example.com")
                .enabled(false)
                .build();

        User user = mapper.toDomain(kcUser);

        assertFalse(user.isActive());
    }

    @Test
    void mapsUserWithoutFirstOrLastName() {
        KeycloakUserResponse kcUser = KeycloakUserResponse.builder()
                .id(UUID.randomUUID().toString())
                .username("nobody")
                .firstName(null)
                .lastName(null)
                .email("nobody@example.com")
                .enabled(true)
                .build();

        User user = mapper.toDomain(kcUser);

        assertEquals("nobody", user.getDisplayName());
    }
}
