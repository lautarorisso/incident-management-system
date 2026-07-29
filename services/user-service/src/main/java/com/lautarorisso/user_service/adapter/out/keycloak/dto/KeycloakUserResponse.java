package com.lautarorisso.user_service.adapter.out.keycloak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a Keycloak user from the Admin REST API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakUserResponse {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private boolean enabled;
    private Map<String, List<String>> attributes;
}
