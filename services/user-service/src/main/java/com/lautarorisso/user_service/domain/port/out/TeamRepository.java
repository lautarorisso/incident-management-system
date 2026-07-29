package com.lautarorisso.user_service.domain.port.out;

import com.lautarorisso.user_service.domain.model.Team;
import com.lautarorisso.user_service.domain.model.TeamId;

import java.util.List;
import java.util.Optional;

/**
 * Driven port (outbound) for persisting and retrieving Teams.
 * <p>
 * Teams are synced from Keycloak groups and cached locally.
 */
public interface TeamRepository {

    Team save(Team team);

    Optional<Team> findById(TeamId id);

    Optional<Team> findByName(String name);

    List<Team> findAll();

    void deleteById(TeamId id);

    boolean existsByName(String name);
}
