package com.lautarorisso.user_service.adapter.out.keycloak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO representing a Keycloak group from the Admin REST API.
 */
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakGroupResponse {
    private String id;
    private String name;
    private String path;
}
