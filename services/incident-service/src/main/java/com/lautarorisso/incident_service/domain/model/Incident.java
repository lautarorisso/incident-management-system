package com.lautarorisso.incident_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Core domain model representing an Incident.
 * <p>
 * An Incident tracks an operational event from creation through resolution
 * and closure. It is a pure domain object with no infrastructure concerns.
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    private IncidentId id;
    private String title;
    private String description;

    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    @Builder.Default
    private IncidentPriority priority = IncidentPriority.MEDIUM;

    private UUID assigneeId;
    private UUID teamId;
    private Instant createdAt;
    private Instant updatedAt;
}
