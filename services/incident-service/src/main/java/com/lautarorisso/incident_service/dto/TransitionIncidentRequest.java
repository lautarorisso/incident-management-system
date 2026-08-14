package com.lautarorisso.incident_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * REST request body for transitioning an Incident to a new status.
 */
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to transition an incident to a new status")
public class TransitionIncidentRequest {

    @NotBlank(message = "New status is required")
    @Schema(description = "Target status: OPEN, IN_PROGRESS, RESOLVED, or CLOSED",
            example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newStatus;
}
