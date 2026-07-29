package com.lautarorisso.incident_service.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * REST request body for creating a new Incident.
 */
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to create a new incident")
public class CreateIncidentRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Incident title", example = "Database connection pool exhausted", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Schema(description = "Incident description", example = "The connection pool for the primary database has been exhausted, causing connection timeouts")
    private String description;

    @Schema(description = "Incident priority (default: MEDIUM)", example = "HIGH", defaultValue = "MEDIUM")
    private String priority;
}
