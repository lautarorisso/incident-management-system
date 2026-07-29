package com.lautarorisso.user_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Core domain model representing a User synced from Keycloak.
 * <p>
 * A User has a profile managed in Keycloak with application-level
 * data stored locally. Team memberships are tracked via TeamId references.
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private UserId id;
    private UUID keycloakId;
    private String username;
    private String displayName;
    private String email;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private List<TeamId> teamIds = Collections.emptyList();

    private Instant createdAt;
    private Instant updatedAt;
}
