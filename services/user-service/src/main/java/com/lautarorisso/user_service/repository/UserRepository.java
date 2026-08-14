package com.lautarorisso.user_service.repository;

import com.lautarorisso.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> findByTeamIdsContaining(UUID teamId);
}
