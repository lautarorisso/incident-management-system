package com.lautarorisso.incident_service.adapter.out.feign.dto;

import java.util.UUID;

/**
 * DTO representing a user from the User Service.
 */
public record UserDto(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String email,
        UUID teamId,
        boolean active
) {}
