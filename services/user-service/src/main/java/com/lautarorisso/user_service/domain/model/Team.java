package com.lautarorisso.user_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

/**
 * Core domain model representing a Team synced from Keycloak groups.
 * <p>
 * Teams map to Keycloak groups and are synced periodically.
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    private TeamId id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
