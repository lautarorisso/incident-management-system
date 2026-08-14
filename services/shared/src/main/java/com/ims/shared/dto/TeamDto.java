package com.ims.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Cross-service DTO representing a team from the User Service.
 * <p>
 * Used by incident-service (via Feign client) to resolve team details.
 */
@Schema(description = "Team shared across services")
public record TeamDto(
        @Schema(description = "Unique identifier of the team") UUID id,
        @Schema(description = "Team name") String name,
        @Schema(description = "Team description") String description,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Last update timestamp") Instant updatedAt
) {}
