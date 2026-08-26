package com.lautarorisso.incident_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Full REST response representation of an Incident.
 */
@Schema(description = "Full incident details")
public record IncidentResponse(
        @Schema(description = "Incident UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Incident title", example = "Database connection pool exhausted")
        String title,

        @Schema(description = "Incident description", example = "The connection pool for the primary database has been exhausted")
        String description,

        @Schema(description = "Current status", example = "OPEN")
        String status,

        @Schema(description = "Priority level", example = "HIGH")
        String priority,

        @Schema(description = "UUID of the assigned user", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID assigneeId,

        @Schema(description = "UUID of the assigned team", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID teamId,

        @Schema(description = "Creation timestamp", example = "2026-07-29T10:30:00Z")
        Instant createdAt,

        @Schema(description = "Last update timestamp", example = "2026-07-29T10:35:00Z")
        Instant updatedAt
) {
}
