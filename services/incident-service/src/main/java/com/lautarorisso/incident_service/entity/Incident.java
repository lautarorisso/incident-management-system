package com.lautarorisso.incident_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lautarorisso.incident_service.enums.IncidentPriority;
import com.lautarorisso.incident_service.enums.IncidentStatus;

/**
 * JPA entity and rich domain model for an incident.
 * <p>
 * The entity owns its invariants and mutation behavior: creation happens
 * through {@link #open(String, String, IncidentPriority)}, assignment through
 * {@link #assignTo(UUID, UUID)}, and lifecycle changes through
 * {@link #changeStatus(IncidentStatus)}. All mutators set {@code updatedAt}
 * themselves; there are no public setters.
 */
@Entity
@Table(name = "incidents")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentPriority priority;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // --- state machine ---

    private static final Map<IncidentStatus, Set<IncidentStatus>> VALID_TRANSITIONS = new EnumMap<>(IncidentStatus.class);

    static {
        VALID_TRANSITIONS.put(IncidentStatus.OPEN, EnumSet.of(IncidentStatus.IN_PROGRESS));
        VALID_TRANSITIONS.put(IncidentStatus.IN_PROGRESS, EnumSet.of(IncidentStatus.RESOLVED));
        VALID_TRANSITIONS.put(IncidentStatus.RESOLVED, EnumSet.of(IncidentStatus.CLOSED, IncidentStatus.OPEN));
        VALID_TRANSITIONS.put(IncidentStatus.CLOSED, EnumSet.noneOf(IncidentStatus.class));
    }

    // --- static factory ---

    /**
     * Blessed creation path for a new incident.
     *
     * @param title       the incident title (trimmed, must not be blank)
     * @param description optional description (trimmed if non-null)
     * @param priority    desired priority; defaults to {@link IncidentPriority#MEDIUM}
     * @return a new {@link Incident} with status {@link IncidentStatus#OPEN}
     * @throws IllegalArgumentException if title is null or blank
     */
    public static Incident open(String title, String description, IncidentPriority priority) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        IncidentPriority resolvedPriority = priority != null ? priority : IncidentPriority.MEDIUM;
        Instant now = Instant.now();
        return Incident.builder()
                .id(UUID.randomUUID())
                .title(title.trim())
                .description(description != null ? description.trim() : null)
                .status(IncidentStatus.OPEN)
                .priority(resolvedPriority)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    // --- behavior methods ---

    /**
     * Assigns this incident to a user and optional team.
     *
     * @param assigneeId the active user ID (must not be null)
     * @param teamId     optional team ID (may be null)
     * @throws IllegalArgumentException if assigneeId is null
     */
    public void assignTo(UUID assigneeId, UUID teamId) {
        if (assigneeId == null) {
            throw new IllegalArgumentException("Assignee ID must not be null");
        }
        this.assigneeId = assigneeId;
        this.teamId = teamId;
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions this incident to a new status, validating the state-machine rules.
     *
     * @param newStatus the target status (must not be null)
     * @throws IllegalArgumentException if newStatus is null
     * @throws IllegalStateException    if the transition is not allowed
     */
    public void changeStatus(IncidentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Target status must not be null");
        }
        if (!isValidTransition(this.status, newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Checks whether a status transition is valid according to the state machine.
     */
    public boolean isValidTransition(IncidentStatus from, IncidentStatus to) {
        Set<IncidentStatus> allowed = VALID_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
}
