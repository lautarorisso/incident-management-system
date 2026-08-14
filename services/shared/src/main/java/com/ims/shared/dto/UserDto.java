package com.ims.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cross-service DTO representing a user from the User Service.
 * <p>
 * Used by incident-service (via Feign client) to resolve assignee details.
 */
@Schema(description = "User profile shared across services")
public record UserDto(
        @Schema(description = "Unique identifier of the user") UUID id,
        @Schema(description = "Keycloak user identifier") UUID keycloakId,
        @Schema(description = "Username for login") String username,
        @Schema(description = "Display name for UI") String displayName,
        @Schema(description = "Email address") String email,
        @Schema(description = "Whether the user account is active") boolean active,
        @Schema(description = "Team IDs the user belongs to") List<UUID> teamIds,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt
) {}
