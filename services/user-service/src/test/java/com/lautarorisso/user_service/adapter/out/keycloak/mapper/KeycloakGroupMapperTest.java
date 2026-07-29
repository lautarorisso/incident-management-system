package com.lautarorisso.user_service.adapter.out.keycloak.mapper;

import com.lautarorisso.user_service.adapter.out.keycloak.dto.KeycloakGroupResponse;
import com.lautarorisso.user_service.domain.model.Team;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakGroupMapperTest {

    private final KeycloakGroupMapper mapper = new KeycloakGroupMapper();

    @Test
    void mapsKeycloakGroupToDomainTeam() {
        String groupId = UUID.randomUUID().toString();
        KeycloakGroupResponse kcGroup = KeycloakGroupResponse.builder()
                .id(groupId)
                .name("SRE")
                .path("/SRE")
                .build();

        Team team = mapper.toDomain(kcGroup);

        assertNull(team.getId()); // ID is assigned by persistence layer
        assertEquals("SRE", team.getName());
        assertEquals("Keycloak group: /SRE", team.getDescription());
    }

    @Test
    void mapsGroupWithoutPath() {
        KeycloakGroupResponse kcGroup = KeycloakGroupResponse.builder()
                .id(UUID.randomUUID().toString())
                .name("Engineering")
                .build();

        Team team = mapper.toDomain(kcGroup);

        assertEquals("Engineering", team.getName());
    }
}
