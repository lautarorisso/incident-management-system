package com.lautarorisso.incident_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * REST request body for creating a new Incident.
 */
@Schema(description = "Request payload to create a new incident")
public record CreateIncidentRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @Schema(description = "Incident title", example = "Database connection pool exhausted", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        @Schema(description = "Incident description", example = "The connection pool for the primary database has been exhausted, causing connection timeouts")
        String description,

        @Schema(description = "Incident priority (default: MEDIUM)", example = "HIGH", defaultValue = "MEDIUM")
        String priority
) {
}
