package com.lautarorisso.user_service.adapter.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST response DTO for a User.
 */
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile response")
public class UserResponse {
    private UUID id;
    private UUID keycloakId;
    private String username;
    private String displayName;
    private String email;
    private boolean active;
    private List<UUID> teamIds;
    private Instant createdAt;
    private Instant updatedAt;
}
