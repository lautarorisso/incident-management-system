package com.lautarorisso.incident_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * REST request body for assigning an Incident to a user and/or team.
 */
@Schema(description = "Request payload to assign an incident to a user and/or team")
public record AssignIncidentRequest(
        @NotNull(message = "Assignee ID is required")
        @Schema(description = "UUID of the user to assign", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID assigneeId,

        @Schema(description = "UUID of the team the assignee belongs to", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        UUID teamId
) {
}
