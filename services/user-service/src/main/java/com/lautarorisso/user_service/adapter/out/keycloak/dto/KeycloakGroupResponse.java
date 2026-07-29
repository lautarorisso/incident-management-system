package com.lautarorisso.user_service.adapter.out.keycloak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO representing a Keycloak group from the Admin REST API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakGroupResponse {
    private String id;
    private String name;
    private String path;
    private Map<String, String[]> attributes;
}
