package com.lautarorisso.user_service.domain.port.out;

import com.lautarorisso.user_service.domain.model.TeamId;
import com.lautarorisso.user_service.domain.model.User;
import com.lautarorisso.user_service.domain.model.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Driven port (outbound) for persisting and retrieving Users.
 * <p>
 * The application layer calls this interface; the infrastructure layer
 * provides the implementation (e.g. JPA adapter).
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByKeycloakId(UUID keycloakId);

    List<User> findAll();

    List<User> findByTeamId(TeamId teamId);

    void deleteById(UserId id);

    boolean existsByKeycloakId(UUID keycloakId);
}
