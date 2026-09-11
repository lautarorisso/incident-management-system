package com.lautarorisso.incident_service.client;

import com.ims.shared.dto.TeamDto;
import com.ims.shared.dto.UserDto;
import com.ims.shared.security.FeignTokenRelayConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for communicating with the User Service.
 * <p>
 * Uses a configurable URL (set via {@code user-service.url}) for tests
 * and service discovery in production.
 * <p>
 * {@link FeignTokenRelayConfig} registers the shared relay interceptor on THIS
 * client only: it forwards the incoming request's {@code Authorization} header
 * (read from {@code RequestContextHolder}) to user-service, which validates the
 * same JWT locally (defense-in-depth token relay; no service-account tokens).
 * Downstream failures surface as {@link feign.FeignException} — 4xx pass through
 * with their exact status (401/403 included, never masked as 503) and 5xx /
 * connectivity are mapped to HTTP 503 by {@code GlobalExceptionHandler}.
 */
@FeignClient(
        name = "user-service",
        url = "${user-service.url:}",
        configuration = FeignTokenRelayConfig.class
)
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserDto findUserById(@PathVariable("id") UUID id);

    @GetMapping("/api/teams/{id}")
    TeamDto findTeamById(@PathVariable("id") UUID id);
}
