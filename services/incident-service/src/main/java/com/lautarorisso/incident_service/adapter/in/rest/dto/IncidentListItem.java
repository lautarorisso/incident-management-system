package com.lautarorisso.incident_service.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight REST response representation of an Incident for list views.
 */
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary incident information for list views")
public class IncidentListItem {

    @Schema(description = "Incident UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Incident title", example = "Database connection pool exhausted")
    private String title;

    @Schema(description = "Current status", example = "OPEN")
    private String status;

    @Schema(description = "Priority level", example = "HIGH")
    private String priority;

    @Schema(description = "UUID of the assigned user", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID assigneeId;

    @Schema(description = "Creation timestamp", example = "2026-07-29T10:30:00Z")
    private Instant createdAt;
}
