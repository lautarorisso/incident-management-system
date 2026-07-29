package com.lautarorisso.incident_service.adapter.out.feign.dto;

import java.util.UUID;

/**
 * DTO representing a team from the User Service.
 */
public record TeamDto(
        UUID id,
        String name,
        boolean active
) {}
