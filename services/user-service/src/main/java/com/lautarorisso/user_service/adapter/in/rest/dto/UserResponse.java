package com.lautarorisso.user_service.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST response DTO for a User.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User profile response")
public class UserResponse {
    private UUID id;
    private UUID keycloakId;
    private String username;
    private String displayName;
    private String email;
    private boolean active;
    private List<UUID> teamIds;
    private Instant createdAt;
    private Instant updatedAt;
}
