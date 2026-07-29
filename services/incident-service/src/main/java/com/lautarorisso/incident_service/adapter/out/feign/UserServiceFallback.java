package com.lautarorisso.incident_service.adapter.out.feign;

import com.lautarorisso.incident_service.adapter.out.feign.dto.TeamDto;
import com.lautarorisso.incident_service.adapter.out.feign.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback factory for UserServiceClient.
 * <p>
 * Returns null for all calls when the user-service is unavailable
 * or the circuit breaker is open. The calling code interprets null
 * as "user/team not found or service unavailable".
 */
@Slf4j
@Component
public class UserServiceFallback implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        log.warn("UserServiceClient fallback triggered: {}", cause.getMessage());
        return new UserServiceClient() {
            @Override
            public UserDto findUserById(UUID id) {
                log.debug("Fallback: findUserById({})", id);
                return null;
            }

            @Override
            public TeamDto findTeamById(UUID id) {
                log.debug("Fallback: findTeamById({})", id);
                return null;
            }
        };
    }
}
