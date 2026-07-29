package com.lautarorisso.user_service.adapter.out.sync;

import com.lautarorisso.user_service.adapter.out.keycloak.mapper.KeycloakGroupMapper;
import com.lautarorisso.user_service.adapter.out.keycloak.mapper.KeycloakUserMapper;
import com.lautarorisso.user_service.domain.port.out.KeycloakAdminClient;
import com.lautarorisso.user_service.domain.port.out.TeamRepository;
import com.lautarorisso.user_service.domain.port.out.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Scheduled task that syncs users and groups from Keycloak.
 * <p>
 * Runs an initial sync on startup, then periodically every 5 minutes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakSyncScheduler {

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final KeycloakUserMapper userMapper;
    private final KeycloakGroupMapper groupMapper;

    @PostConstruct
    public void initialSync() {
        log.info("Running initial Keycloak sync on startup");
        syncUsers();
        syncGroups();
        log.info("Initial Keycloak sync completed");
    }

    @Scheduled(fixedDelay = 300_000) // 5 minutes
    public void periodicSync() {
        log.info("Running periodic Keycloak sync");
        syncUsers();
        syncGroups();
        log.info("Periodic Keycloak sync completed");
    }

    @Transactional
    public void syncUsers() {
        var kcUsers = keycloakAdminClient.fetchUsers();
        log.debug("Fetched {} users from Keycloak", kcUsers.size());

        for (var kcUser : kcUsers) {
            UUID keycloakId = UUID.fromString(kcUser.getId());
            if (!userRepository.existsByKeycloakId(keycloakId)) {
                var user = userMapper.toDomain(kcUser);
                userRepository.save(user);
                log.debug("Saved new user: {}", kcUser.getUsername());
            }
        }
    }

    @Transactional
    public void syncGroups() {
        var kcGroups = keycloakAdminClient.fetchGroups();
        log.debug("Fetched {} groups from Keycloak", kcGroups.size());

        for (var kcGroup : kcGroups) {
            if (!teamRepository.existsByName(kcGroup.getName())) {
                var team = groupMapper.toDomain(kcGroup);
                teamRepository.save(team);
                log.debug("Saved new team: {}", kcGroup.getName());
            }
        }
    }
}
