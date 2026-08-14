package com.lautarorisso.incident_service.client;

import com.ims.shared.dto.TeamDto;
import com.ims.shared.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for communicating with the User Service.
 * <p>
 * Uses a configurable URL (set via {@code user-service.url}) for tests
 * and service discovery in production. Downstream failures surface as
 * {@link feign.FeignException} and are mapped to HTTP 503 by
 * {@code GlobalExceptionHandler}.
 */
@FeignClient(
        name = "user-service",
        url = "${user-service.url:}"
)
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserDto findUserById(@PathVariable("id") UUID id);

    @GetMapping("/api/teams/{id}")
    TeamDto findTeamById(@PathVariable("id") UUID id);
}
