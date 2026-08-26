package com.lautarorisso.incident_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * REST request body for transitioning an Incident to a new status.
 */
@Schema(description = "Request payload to transition an incident to a new status")
public record TransitionIncidentRequest(
        @NotBlank(message = "New status is required")
        @Schema(description = "Target status: OPEN, IN_PROGRESS, RESOLVED, or CLOSED",
                example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
        String newStatus
) {
}
